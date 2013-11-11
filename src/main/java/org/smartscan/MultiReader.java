package org.smartscan;


import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.reference.MatchPattern;
import org.smartscan.reference.ScannerTools;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class MultiReader
    extends ModernBase
{

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    private final ExecutorService executor;

    private final FastFileReceiver fastFileReceiver;

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiReader( File basedir, String[] includes, String[] excludes, FastFileReceiver fastFileReceiver,
                        int nThreads )
    {
        super( basedir, includes, excludes );
        ScannerTools.verifyBaseDir( basedir );
        executor = Executors.newFixedThreadPool( nThreads );
        this.fastFileReceiver = fastFileReceiver;
    }

    public void getScanResult()
    {
        while ( threadsStarted.get() > 0 )
        {
            ;
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
        executor.submit( scanner );
    }

    private void asynchscandir( File dir, String vpath )
    {
        scandir( dir, vpath );
        threadsStarted.decrementAndGet();
    }


    private void scandir( File dir, String vpath )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            newfiles = new String[0];
        }

        File firstDir = null;
        String firstName = null;
        String[] tokenized = MatchPattern.tokenizePathToString( vpath + "fud", File.separator );
        for ( String newfile : newfiles )
        {
            String currentFullSubPath = vpath + newfile;
            File file = new File( dir, newfile );
            tokenized[tokenized.length - 1] = newfile;
            boolean shouldInclude = shouldInclude( currentFullSubPath, tokenized );
            if ( file.isFile() )
            {
                if ( shouldInclude )
                {
                    fastFileReceiver.accept( new FastFile( currentFullSubPath ) );
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
            scandir( firstDir, firstName + File.separator );
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