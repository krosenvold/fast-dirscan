package org.rosenvold;

import org.mentaqueue.AtomicQueue;
import org.mentaqueue.BatchingQueue;
import org.mentaqueue.util.Builder;
import org.rosenvold.ModernBase;
import org.rosenvold.reference.ScannerTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class ForkedSingleThread
    extends ModernBase
{
    private final ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<String>();

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    public static final String POISON = "*POISON*";

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public ForkedSingleThread( File basedir, String[] includes, String[] excludes )
    {
        super( basedir, includes, excludes );
        ScannerTools.verifyBaseDir( basedir );
    }


    void getScanResult(FastFileReceiver fastFileReceiver)
    {
        List<String> result = new ArrayList<String>();
        String item;
        while ( ( item = queue.poll() ) != POISON )
        {
            if ( item != null )
            {
                if ( isIncluded( item ) )
                {
                    if ( !isExcluded( item ) )
                    {
                        fastFileReceiver.accept( new FastFile( item ) );
                        result.add( item );
                    }
                }
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
                queue.add( POISON );
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
        return thread;
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
        for ( String newfile : newfiles )
        {
            String name = vpath + newfile;
            File file = new File( dir, newfile );
            if ( file.isFile() )
            {
                queue.add( name );
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
                            scandir( file, name + File.separator );
                        }
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstName + File.separator );
        }

    }

}
