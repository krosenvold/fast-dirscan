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
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class MultiReader
    extends ModernBase
{

    public static final char[][] NO_FILES_VPATH_ = new char[0][];

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    private final ForkJoinPool executor;

    private final SmartFileReceiver smartFileReceiver;

    public final AtomicBoolean completed = new AtomicBoolean( false );

    private static final String[] NOFILES = new String[0];

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiReader( File basedir, String[] includes, String[] excludes, SmartFileReceiver smartFileReceiver,
                        int nThreads )
    {
        super( basedir, includes, excludes );
        ScannerTools.verifyBaseDir( basedir );
        executor = new ForkJoinPool( nThreads );
        this.smartFileReceiver = smartFileReceiver;
    }

	public MultiReader( File basedir, MatchPatterns includes, MatchPatterns excludes, SmartFileReceiver smartFileReceiver,
						int nThreads )
	{
		super( basedir, includes, excludes );
		ScannerTools.verifyBaseDir( basedir );
		executor = new ForkJoinPool( nThreads );
		this.smartFileReceiver = smartFileReceiver;
	}


    public void awaitScanResult()
    {
        //noinspection StatementWithEmptyBody
        while ( threadsStarted.get() > 0 )
        {
        }
        executor.shutdown();
    }


    public void scanThreaded()
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
        int i = threadsStarted.decrementAndGet();
        if ( i == 0 )
        {
            completed.set( true );
        }

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
						//sSystem.out.println(file.getPath());
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
                                threadsStarted.incrementAndGet();
                                  //  System.out.println( "executor = " + executor );
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
	final class AsynchScanner extends ForkJoinTask
    {
        File dir;

        char[][] vpath;

        AsynchScanner( File dir, char[][] vpath )
        {
            this.dir = dir;
            this.vpath = vpath;
        }


        @Override
        public Object getRawResult()
        {
            return Boolean.TRUE;
        }

        @Override
        protected void setRawResult( Object value )
        {

        }

        @Override
        protected boolean exec()
        {
            try
            {

                asynchscandir( dir, vpath );
                return true;
            }
            catch ( Throwable e )
            {
                e.printStackTrace();
                return false;
            }
        }
    }

    public void close()
    {
        executor.shutdown();
    }

}