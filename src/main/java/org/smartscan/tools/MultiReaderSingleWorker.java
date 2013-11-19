package org.smartscan.tools;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedTransferQueue;

public class MultiReaderSingleWorker implements Iterator<SmartFile>
{

    private final MultiReader reader;

    private final LinkedTransferQueue<SmartFile> queue;

	public MultiReaderSingleWorker( @Nonnull File basedir, @Nonnull MatchPatterns includes,
									@Nonnull MatchPatterns excludes, int nThreads )
			throws InterruptedException
	{
		queue = new LinkedTransferQueue<>();
		reader = new MultiReader( basedir, includes, excludes, getFastFileReceiver(), nThreads );
		reader.beginThreadedScan();
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

	@SuppressWarnings("StatementWithEmptyBody")
	public boolean hasNext() {
			while ( !reader.isComplete()  && queue.peek() == null ){

			}
			return queue.peek() != null;

	}

	@Override
	public SmartFile next(){
		SmartFile next = queue.poll();
		if (next == null) {
			throw new NoSuchElementException("Illegal state");
		}
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cant do that");
	}


}
