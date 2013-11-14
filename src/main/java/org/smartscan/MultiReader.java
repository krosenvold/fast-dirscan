package org.smartscan;


import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.tools.ScannerTools;
import org.smartscan.tools.SelectorUtils;

import java.io.File;
import java.util.Arrays;
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

    public static char[][] copyWithOneExtra( char[][] original)
    {
        int length = original.length;
        char[][] result = new char[ length + 1][];
        System.arraycopy( original, 0, result, 0, length );
        return result;
    }

    public static char[][] copy( char[][] original)
    {
        int length = original.length;
        char[][] result = new char[ length][];
        System.arraycopy( original, 0, result, 0, length );
        return result;
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
                asynchscandir( basedir, new char[0][] );
            }
        };
        executor.submit( scanner );
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


    private void scandir( File parent, char[][] unmodifyableparentvpath )
    {
        String[] newfiles = parent.list();

        if ( newfiles == null )
        {
            newfiles = NOFILES;
        }

        File firstDir = null;
        char[][] firstVpath = null;

        char[][] mutablevpath = copyWithOneExtra( unmodifyableparentvpath );
        for ( String newfile : newfiles )
        {
            File file = new File( parent, newfile );
            mutablevpath[mutablevpath.length - 1] = newfile.toCharArray();
            boolean shouldInclude = shouldInclude( mutablevpath );
            if ( file.isFile() )
            {
                if ( shouldInclude )
                {
                    fastFileReceiver.accept( new FastFile( file, unmodifyableparentvpath ) );
                }
            }
            else if ( file.isDirectory() )
            {
                if ( shouldInclude || couldHoldIncluded( mutablevpath ) )
                {
                    if ( firstDir == null )
                    {
                        firstDir = file;
                        firstVpath = copy(mutablevpath);
                    }
                    else
                    {

                        final Runnable target = new AsynchScanner( file, copy(mutablevpath) );
                        threadsStarted.incrementAndGet();
                        executor.submit( target );
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstVpath);
        }

    }

    class AsynchScanner
        implements Runnable
    {
        File dir;

        char[][] file;

        AsynchScanner( File dir, char[][] vpath )
        {
            this.dir = dir;
            this.file = vpath;
        }

        @Override
        public void run()
        {
            try {

            asynchscandir( dir, file );
            } catch (Throwable e){
                e.printStackTrace();
            }
        }
    }

    public void close()
    {
        executor.shutdown();
    }

}