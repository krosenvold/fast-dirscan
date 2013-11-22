package org.smartscan.tools;


import org.smartscan.api.Java7SmartFile;
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
public class MultiReader
    extends ModernBase
{

    public static final char[][] NO_FILES_VPATH_ = new char[0][];

    private final ForkJoinPool executor;

    private final SmartFileReceiver smartFileReceiver;

	public MultiReader( File basedir, Filters includes, Filters excludes, SmartFileReceiver smartFileReceiver,
						int nThreads )
	{
		super( basedir, includes, excludes );
		ScannerTools.verifyBaseDir( basedir );
		executor = new ForkJoinPool( nThreads );
		this.smartFileReceiver = smartFileReceiver;
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
				scandir(basedir, NO_FILES_VPATH_);
			}
        };
        executor.submit(scanner);
    }


	@SuppressWarnings( "AssignmentToMethodParameter" )
    private void scandir( File parent, char[][] unmodifyableparentvpath )
    {
        @Nullable File firstDir;
        @Nullable char[][] firstVpath;
        do
        {
            firstDir = null;
            firstVpath = null;

            File[] newfiles = parent.listFiles();

            if ( newfiles != null )
            {
                char[][] mutablevpath = copyWithOneExtra( unmodifyableparentvpath );
                for ( final File file : newfiles )
                {
                    mutablevpath[mutablevpath.length - 1] = file.getName().toCharArray();
                    BasicFileAttributes basicFileAttributes;
                    try
                    {
                        basicFileAttributes = Files.readAttributes( file.toPath(), BasicFileAttributes.class );
                    }
                    catch ( IOException e )
                    {
						throw new RuntimeException(e);
                    }
                    boolean shouldInclude = shouldInclude( mutablevpath );
                    if ( basicFileAttributes.isRegularFile() )
                    {
                        if ( shouldInclude )
                        {
                            smartFileReceiver.accept(Java7SmartFile.createSmartFile(file, unmodifyableparentvpath, basicFileAttributes));
                        }
                    }
                    else if ( basicFileAttributes.isDirectory() )
                    {
                        if ( shouldInclude || couldHoldIncluded( mutablevpath ) )
                        {
                            if ( firstDir == null )
                            {
                                firstDir = file;
                                firstVpath = copy( mutablevpath );
                            }
                            else
                            {
								final char[][] copy = copy(mutablevpath);
								new RecursiveAction() {
									@Override
									protected void compute() {
										scandir(file, copy);
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
}