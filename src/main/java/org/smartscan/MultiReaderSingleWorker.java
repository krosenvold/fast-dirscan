package org.smartscan;

import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;

import java.io.File;
import java.util.concurrent.LinkedTransferQueue;

public class MultiReaderSingleWorker
{

    private final MultiReader reader;

    protected final LinkedTransferQueue<FastFile> queue;


    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public MultiReaderSingleWorker( File basedir, String[] includes, String[] excludes, int nThreads )
        throws InterruptedException
    {
        queue = new LinkedTransferQueue<>();
        reader = new MultiReader( basedir, includes, excludes, getFastFileReceiver(), nThreads );
        reader.scanThreaded();
    }


    public void getScanResult( FastFileReceiver fastFileReceiver )
    {
        FastFile name;
        while ( !reader.completed.get() )
        {
            while ( ( name = queue.poll() ) != null )
            {
                fastFileReceiver.accept( name );
            }
        }
    }

    private FastFileReceiver getFastFileReceiver()
    {
        return new FastFileReceiver()
        {
            @Override
            public void accept( FastFile file )
            {
                queue.add( file);
            }
        };
    }

    public void close()
    {
        reader.close();
    }
}
