package org.smartscan.tools;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.LinkedTransferQueue;

public class MultiReaderSingleWorker
{

    private final MultiReader reader;

    protected final LinkedTransferQueue<SmartFile> queue;


    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
	public MultiReaderSingleWorker( @Nonnull File basedir, @Nonnull MatchPatterns includes,
									@Nonnull MatchPatterns excludes, int nThreads )
			throws InterruptedException
	{
		queue = new LinkedTransferQueue<>();
		reader = new MultiReader( basedir, includes, excludes, getFastFileReceiver(), nThreads );
		reader.beginThreadedScan();
	}

    public void getScanResult( SmartFileReceiver smartFileReceiver)
    {
        SmartFile name;
        while ( !reader.isComplete())
        {
            while ( ( name = queue.poll() ) != null )
            {
                smartFileReceiver.accept( name );
            }
        }
    }

    private SmartFileReceiver getFastFileReceiver()
    {
        return new SmartFileReceiver()
        {
            @Override
            public void accept( SmartFile file )
            {
                queue.add( file);
            }
        };
    }

	public boolean hasNext() {
			while ( !reader.isComplete()  && queue.peek() == null )
				;
			return queue.peek() != null;

	}

	public SmartFile next(){
		return queue.poll();
	}
}
