package org.smartscan.tools;


import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Reads with multiple threads
 */
public class CachingMultiReader
    extends ModernBase
{

    public static final char[][] NO_FILES_VPATH_ = new char[0][];

    private final ForkJoinPool executor;

    private final SmartFileReceiver smartFileReceiver;

	private final ScanCache scanCache;

	public CachingMultiReader(File basedir, Filters includes, Filters excludes, SmartFileReceiver smartFileReceiver,
							  int nThreads)
	{
		super( basedir, includes, excludes );
		ScannerTools.verifyBaseDir( basedir );
		executor = new ForkJoinPool( nThreads );
		this.smartFileReceiver = smartFileReceiver;
		scanCache = new ScanCache();
	}

	public boolean isComplete(){
		final boolean quiescent = executor.isQuiescent();
		if (quiescent){
			executor.shutdown();
		}
		return quiescent;
	}

    @SuppressWarnings("StatementWithEmptyBody")
	public void awaitCompletion()
    {
		while (!executor.isQuiescent()) {doSleep(1);}
        executor.shutdown();
    }

	public void beginThreadedScan()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            @Override
            public void run()
            {
				scandir(Java7SmartFile.createSmartFile(basedir, NO_FILES_VPATH_), NO_FILES_VPATH_);
			}
        };
        executor.submit(scanner);
    }


	@SuppressWarnings( "AssignmentToMethodParameter" )
    private void scandir( SmartFile parent, char[][] unmodifyableparentvpath )
    {
        @Nullable SmartFile firstDir;
        @Nullable char[][] firstVpath;
        do
        {
            firstDir = null;
            firstVpath = null;

            File[] newfiles = parent.listFiles();

            if ( newfiles != null )
            {
				final SmartFile[] smartFiles = createSmartFiles(newfiles, unmodifyableparentvpath);
				char[][] mutablevpath = copyWithOneExtra(unmodifyableparentvpath);
				for (final SmartFile smartFile : smartFiles) {
					mutablevpath[mutablevpath.length - 1] = smartFile.getFile().getName().toCharArray();

                    boolean shouldInclude = shouldInclude( mutablevpath );
                    if ( smartFile.isFile() )
                    {
                        if ( shouldInclude )
                        {
                            smartFileReceiver.accept(smartFile);
                        }
                    }
                    else if ( smartFile.isDirectory() )
                    {
                        if ( shouldInclude || couldHoldIncluded( mutablevpath ) )
                        {
                            if ( firstDir == null )
                            {
                                firstDir = smartFile;
                                firstVpath = copy( mutablevpath );
                            }
                            else
                            {
								final char[][] copy = copy(mutablevpath);
								new RecursiveAction() {
									@Override
									protected void compute() {
										scandir(smartFile, copy);
									}
								}.fork();
								// Todo: fix swallowed exceptions
                            }
                        }
                    }
                }
            }
            parent = firstDir;
            unmodifyableparentvpath = firstVpath;
        }
        while ( firstDir != null );
    }

	private static SmartFile[] createSmartFiles(File[] files, char[][] unmodifyableparentvpath){

		final int length = files.length;
		SmartFile[] result = new SmartFile[length];

		for (int i = 0; i < length; i++)
		{
			result[i] = Java7SmartFile.createSmartFile(files[i], unmodifyableparentvpath);
		}
		return result;
	}
}