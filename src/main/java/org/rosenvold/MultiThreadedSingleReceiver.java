package org.rosenvold;


import org.rosenvold.reference.ScannerTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class MultiThreadedSingleReceiver
    extends ModernBase
{

    private final ConcurrentLinkedQueue<String> queue;

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    private boolean isCaseSensitive = true;

    private final ExecutorService executor;

    public static final String POISON = "*POISON*";


    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiThreadedSingleReceiver( File basedir, String[] includes, String[] excludes, int nThreads )
    {
        super( basedir, includes, excludes );

        queue = new ConcurrentLinkedQueue();
        ScannerTools.verifyBaseDir( basedir );
        executor = Executors.newFixedThreadPool( nThreads );

    }

    void getScanResult( FastFileReceiver fastFileReceiver )
    {
        String name;
        while ( true )
        {
            name = queue.poll();
            if ( name != null )
            {
                if ( name.equals( POISON ) )
                {
                    return;
                }

                if ( isIncluded( name ) && !isExcluded( name ))
                {
                    fastFileReceiver.accept( new FastFile( name ) );
                }
            }
        }
    }


    public ConcurrentLinkedQueue<String> getQueue()
    {
        return queue;
    }


    /**
     * Scans the base directory for files which match at least one include
     * pattern and don't match any exclude patterns. If there are selectors
     * then the files must pass muster there, as well.
     *
     * @throws IllegalStateException if the base directory was set
     *                               incorrectly (i.e. if it is <code>null</code>, doesn't exist,
     *                               or isn't a directory).
     */
    public void scan()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            public void run()
            {
                asynchscandir( basedir, "" );
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
        thread.join();
        while ( threadsStarted.get() > 0 )
        {
            Thread.sleep( 10 );
        }
    }

    public void scanThreaded()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            public void run()
            {
                asynchscandir( basedir, "" );
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
    }

    private void asynchscandir( File dir, String vpath )
    {
        List<String> elementsFound = Collections.synchronizedList( new ArrayList() );
        scandir( dir, vpath, elementsFound );
        queue.addAll( elementsFound );
        if ( threadsStarted.decrementAndGet() == 0 )
        {
            queue.add( POISON );
        }
    }


    private void scandir( File dir, String vpath, List elementsFound )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            newfiles = new String[0];
        }

        File firstDir = null;
        String firstName = null;
        for ( String newfile : newfiles )
        {
            String name = vpath + newfile;
            File file = new File( dir, newfile );
            if ( file.isFile() )
            {
                if ( isIncluded( name ) )
                {
                    if ( !isExcluded( name ) )
                    {
                        elementsFound.add( name );
                    }
                }
            }
            else if ( file.isDirectory() )
            {
                final boolean couldHoldIncluded = couldHoldIncluded( name );
                if ( isIncluded( name ) || couldHoldIncluded )
                {
                    if ( !isExcluded( name ) || couldHoldIncluded )
                    {
                        if ( firstDir == null )
                        {
                            firstDir = file;
                            firstName = name;
                        }
                        else
                        {
                            final Runnable target = new AsynchScanner( file, name + File.separator );
                            executor.submit( target );
                            threadsStarted.incrementAndGet();
                        }
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstName + File.separator, elementsFound );
        }

    }

    class AsynchScanner
        implements Runnable
    {
        File dir;

        String file;

        AsynchScanner( File dir, String file )
        {
            this.dir = dir;
            this.file = file;
        }

        public void run()
        {
            asynchscandir( dir, file );
        }
    }

    public void close()
    {
        executor.shutdown();
    }

}