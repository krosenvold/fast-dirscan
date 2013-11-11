package org.smartscan;

import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.reference.MatchPattern;
import org.smartscan.reference.ScannerTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiReaderSingleWorker
    extends ModernBase
{

    protected final LinkedTransferQueue<String> queue;

    protected final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    protected final ExecutorService executor;

    public static final String POISON = "*POISON*";

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiReaderSingleWorker( File basedir, String[] includes, String[] excludes, int nThreads )
    {
        super( basedir, includes, excludes );

        queue = new LinkedTransferQueue();
        ScannerTools.verifyBaseDir( basedir );
        executor = Executors.newFixedThreadPool( nThreads );

    }


    protected void asynchscandir( File dir, String vpath )
    {
        List<String> elementsFound = Collections.synchronizedList( new ArrayList() );
        scandir( dir, vpath, elementsFound );
        queue.addAll( elementsFound );
        if ( threadsStarted.decrementAndGet() == 0 )
        {
            queue.add( POISON );
        }
    }

    private void scandir( File dir, String vpath, List<String> elementsFound )
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
            String currentFullSubPath = vpath + newfile;
            File file = new File( dir, newfile );
            String[] tokenized = MatchPattern.tokenizePathToString( currentFullSubPath, File.separator );
            boolean shouldInclude = shouldInclude( currentFullSubPath, tokenized );

            if ( file.isFile() )
            {
                if ( shouldInclude )
                {
                    elementsFound.add( currentFullSubPath );
                }
            }
            else if ( file.isDirectory() )
            {
                if ( shouldInclude || couldHoldIncluded( currentFullSubPath, tokenized ) )

                {
                    if ( firstDir == null )
                    {
                        firstDir = file;
                        firstName = currentFullSubPath;
                    }
                    else
                    {
                        final Runnable target = new AsynchScanner( file, currentFullSubPath + File.separator );
                        executor.submit( target );
                        threadsStarted.incrementAndGet();
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstName + File.separator, elementsFound );
        }
    }

    protected class AsynchScanner
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

    public void close()
    {
        executor.shutdown();
    }

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

                if ( isIncluded( name ) && !isExcluded( name ) )
                {
                    fastFileReceiver.accept( new FastFile( name ) );
                }
            }
        }
    }

}
