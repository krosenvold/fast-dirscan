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
{

    private final MultiReader reader;

    protected final LinkedTransferQueue<String> queue;


    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiReaderSingleWorker( File basedir, String[] includes, String[] excludes, int nThreads )
        throws InterruptedException
    {
        queue = new LinkedTransferQueue<>(  );
        reader = new MultiReader( basedir, includes, excludes, getFastFileReceiver(), nThreads );
        reader.scanThreaded();
    }



    public void getScanResult( FastFileReceiver fastFileReceiver )
    {
        String name;
        while ( !reader.completed.get())
        {
            name = queue.poll();
            if ( name != null )
            {
                if ( name.equals( ModernBase.POISON ) )
                {
                    return;
                }

                fastFileReceiver.accept( new FastFile( name ) );
            }
        }
    }

    private FastFileReceiver getFastFileReceiver(){
        return new FastFileReceiver()
        {
            @Override
            public void accept( FastFile file )
            {
                queue.add(  file.getFileName() );
            }
        };
    }

    public void close()
    {
        reader.close();
    }
}
