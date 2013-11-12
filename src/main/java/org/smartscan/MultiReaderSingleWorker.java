package org.smartscan;

import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;

import java.io.File;
import java.util.concurrent.LinkedTransferQueue;

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
        queue = new LinkedTransferQueue<>();
        reader = new MultiReader( basedir, includes, excludes, getFastFileReceiver(), nThreads );
        reader.scanThreaded();
    }


    public void getScanResult( FastFileReceiver fastFileReceiver )
    {
        String name;
        while ( !reader.completed.get() )
        {
            while ( ( name = queue.poll() ) != null )
            {
                fastFileReceiver.accept( new FastFile( name ) );
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
                queue.add( file.getFileName() );
            }
        };
    }

    public void close()
    {
        reader.close();
    }
}
