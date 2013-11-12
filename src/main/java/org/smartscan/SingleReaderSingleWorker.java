package org.smartscan;

import org.mentaqueue.AtomicQueue;
import org.mentaqueue.BatchingQueue;
import org.mentaqueue.util.Builder;
import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.reference.MatchPattern;
import org.smartscan.reference.MatchPatterns;
import org.smartscan.reference.ScannerTools;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class SingleReaderSingleWorker
    extends ModernBase
{
    final BatchingQueue<StringBuilder> queue = new AtomicQueue<StringBuilder>( 2048, new Builder<StringBuilder>()
    {
        public StringBuilder newInstance()
        {
            return new StringBuilder( 1024 );
        }
    } );

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public SingleReaderSingleWorker( File basedir, String[] includes, String[] excludes )
    {
        super( basedir, includes, excludes );
        ScannerTools.verifyBaseDir( basedir );
    }


    void getScanResult( FastFileReceiver fastFileReceiver )
    {
        StringBuilder item;
        while ( true )
        {
            long avail = queue.availableToPoll();
            if ( avail > 0 )
            {
                for ( int i = 0; i < avail; i++ )
                {
                    item = queue.poll();
                    if ( item != null )
                    {
                        String name = item.toString();
                        if ( name.equals( POISON ) )
                        {
                            return;
                        }
                        if ( isIncluded( name ) )
                        {
                            if ( !isExcluded( name ) )
                            {
                                fastFileReceiver.accept( new FastFile( name ) );
                            }
                        }
                    }
                }
                queue.donePolling();
            }
        }
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
                scandir( basedir, "" );
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

    public Thread scanThreaded()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            public void run()
            {
                scandir( basedir, "" );
                StringBuilder sb = queue.nextToDispatch();
                while ( sb == null )
                {
                    doSleep( 10 );
                    sb = queue.nextToDispatch();
                }
                sb.setLength( 0 );
                sb.append( POISON );
                queue.flush();
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
        return thread;
    }


    private void scandir( File dir, String vpath )
    {
        String[] newfiles = dir.list();

        if ( newfiles != null )
        {
            for ( String newfile : newfiles )
            {
                String currentFullSubPath = vpath + newfile;
                File file = new File( dir, newfile );
                if ( file.isFile() )
                {
                    StringBuilder sb = queue.nextToDispatch();
                    while ( sb == null )
                    {
                        doSleep( 10 );
                        sb = queue.nextToDispatch();
                    }
                    sb.setLength( 0 );
                    sb.append( currentFullSubPath );
                    queue.flush();
                }
                else if ( file.isDirectory() )
                {
                    String[] tokenized = MatchPattern.tokenizePathToString( currentFullSubPath, File.separator );
                    char[][] dbl = MatchPatterns.toChars( tokenized );

                    boolean shouldInclude = shouldInclude( currentFullSubPath, dbl );
                    if ( shouldInclude || couldHoldIncluded( currentFullSubPath, tokenized ) )
                    {
                        scandir( file, currentFullSubPath + File.separator );
                    }
                }
            }
        }
    }

}
