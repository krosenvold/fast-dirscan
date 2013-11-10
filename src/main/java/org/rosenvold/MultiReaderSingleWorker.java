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
 * Reads with multiple threads, pipes thru single caller thread
 */
public class MultiReaderSingleWorker
    extends MultiThreadedScanner
{

    public MultiReaderSingleWorker( File basedir, String[] includes, String[] excludes, int nThreads )
    {
        super( basedir, includes, excludes, nThreads );
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
}