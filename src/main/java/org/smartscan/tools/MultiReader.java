package org.smartscan.tools;


import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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

  //  private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    private final ForkJoinPool executor;

    private final SmartFileReceiver smartFileReceiver;

 //   private final AtomicBoolean completed = new AtomicBoolean( false );

	public MultiReader( File basedir, MatchPatterns includes, MatchPatterns excludes, SmartFileReceiver smartFileReceiver,
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
		while (!executor.isQuiescent()) {}
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
                asynchscandir( basedir, NO_FILES_VPATH_ );
            }
        };
        executor.submit(scanner);
    }

    private void asynchscandir( File dir, char[][] vpath )
    {
        scandir( dir, vpath );
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
                for ( File file : newfiles )
                {
                    mutablevpath[mutablevpath.length - 1] = file.getName().toCharArray();
                    BasicFileAttributes basicFileAttributes;
                    try
                    {
                        basicFileAttributes = Files.readAttributes( file.toPath(), BasicFileAttributes.class );
                    }
                    catch ( InvalidPathException | IOException e )
                    {
						//System.out.println(file.getPath());
						continue;
                    }
                    boolean shouldInclude = shouldInclude( mutablevpath );
                    if ( basicFileAttributes.isRegularFile() )
                    {
                        if ( shouldInclude )
                        {
                            smartFileReceiver.accept( new SmartFile( file, unmodifyableparentvpath ) );
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

                                final AsynchScanner target = new AsynchScanner( file, copy( mutablevpath ) );
                                //threadsStarted.incrementAndGet();
                                  // Todo: appears to swallow exceptions
                                target.fork();
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


    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
	final class AsynchScanner extends RecursiveAction
    {
        private final File dir;

		private final char[][] vpath;

        AsynchScanner( File dir, char[][] vpath )
        {
            this.dir = dir;
            this.vpath = vpath;
        }


		@Override
		protected void compute() {
            try
            {

                asynchscandir( dir, vpath );
            }
            catch ( Throwable e )
            {
				throw new RuntimeException(e);
            }
        }
    }


}