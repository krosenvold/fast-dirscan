
package org.rosenvold.multithreadedreaxer;


import org.rosenvold.reference.MatchPatterns;
import org.rosenvold.reference.ScannerTools;
import org.rosenvold.reference.SelectorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class MultiThreadedScannerReader
{
    /**
     * The base directory to be scanned.
     */
    private final File basedir;

    private final ConcurrentLinkedQueue<String> queue;

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    private final MatchPatterns includesPatterns;

    private final MatchPatterns excludesPatterns;

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
    public MultiThreadedScannerReader( File basedir, String[] includes, String[] excludes, int nThreads )
    {
        this.basedir = basedir;
        includesPatterns = MatchPatterns.from( ScannerTools.getIncludes( includes ) );
        excludesPatterns = MatchPatterns.from( ScannerTools.getExcludes( excludes ) );

        queue = new ConcurrentLinkedQueue();
        if ( basedir == null )
        {
            throw new IllegalStateException( "No basedir set" );
        }
        if ( !basedir.exists() )
        {
            throw new IllegalStateException( "basedir " + basedir + " does not exist" );
        }
        if ( !basedir.isDirectory() )
        {
            throw new IllegalStateException( "basedir " + basedir + " is not a directory" );
        }

        executor = Executors.newFixedThreadPool( nThreads );

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
        while (threadsStarted.get() > 0){
            Thread.sleep(10);
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

    private void asynchscandir( File dir, String vpath ){
        List<String> elementsFound = new ArrayList();
        scandir(  dir, vpath, elementsFound );
        queue.addAll( elementsFound );
        if (threadsStarted.decrementAndGet() == 0){
            queue.add( POISON );
        }
    }



    private void scandir( File dir, String vpath, List elementsFound )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception
             *     then???)
             */

            /*
            * [jdcasey] (2) is apparently happening to me, as this is killing one of my tests...
            * this is affecting the assembly plugin, fwiw. I will initialize the newfiles array as
            * zero-length for now.
            *
            * NOTE: I can't find the problematic code, as it appears to come from a native method
            * in UnixFileSystem...
            */
            /*
             * [bentmann] A null array will also be returned from list() on NTFS when dir refers to a soft link or
             * junction point whose target is not existent.
             */
            newfiles = new String[0];

            // throw new IOException( "IO error scanning directory " + dir.getAbsolutePath() );
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

    class AsynchScanner implements Runnable {
        File dir;
        String file;

        AsynchScanner( File dir, String file )
        {
            this.dir = dir;
            this.file = file;
        }

        public void run()
        {
            asynchscandir( dir, file);
        }
    }

    protected boolean isIncluded( String name )
    {
       return includesPatterns.matches( name, isCaseSensitive );
    }

    protected boolean couldHoldIncluded( String name )
    {
        return includesPatterns.matchesPatternStart(name, isCaseSensitive);
    }

    protected boolean isExcluded( String name )
    {
        return excludesPatterns.matches( name, isCaseSensitive );
    }
}
