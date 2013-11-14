package org.smartscan;


import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.tools.ScannerTools;
import org.smartscan.tools.SelectorUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public final AtomicBoolean completed = new AtomicBoolean( false );

    private static final String[] NOFILES = new String[0];

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

    public static char[][] tokenizePathToCharArrayWithOneExtra( String path, char separator )
    {
        return SelectorUtils.tokenizePathToCharArray( path, separator, 1 );
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
                asynchscandir( basedir, "" );
            }
        };
        executor.submit( scanner );
    }

    private void asynchscandir( File dir, String vpath )
    {
        scandir( dir, vpath );
        int i = threadsStarted.decrementAndGet();
        if ( i == 0 )
        {
            completed.set( true );
        }

    }


    private void scandir( File dir, String parentvpath )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            newfiles = NOFILES;
        }

        File firstDir = null;
        String firstName = null;
        char[][] mutablevpath = tokenizePathToCharArrayWithOneExtra( parentvpath, File.separatorChar );
        for ( String newfile : newfiles )
        {
            File file = new File( dir, newfile );
            String vpath = parentvpath + newfile;
            mutablevpath[mutablevpath.length - 1] = newfile.toCharArray();
            boolean shouldInclude = shouldInclude( mutablevpath );
            if ( file.isFile() )
            {
                if ( shouldInclude )
                {
                    fastFileReceiver.accept( new FastFile( file, mutablevpath ) ); // Todo: Look at mutablitiy
                }
            }
            else if ( file.isDirectory() )
            {
                if ( shouldInclude || couldHoldIncluded( mutablevpath ) )
                {
                    if ( firstDir == null )
                    {
                        firstDir = file;
                        firstName = vpath;
                    }
                    else
                    {
                        final Runnable target = new AsynchScanner( file, vpath + File.separatorChar );
                        threadsStarted.incrementAndGet();
                        executor.submit( target );
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstName + File.separatorChar );
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

        @Override
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