package org.smartscan;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.MatchPatterns;
import org.smartscan.tools.MultiReader;
import org.smartscan.tools.MultiReaderSingleWorker;
import org.smartscan.tools.ScannerTools;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Kristian Rosenvold
 */
public class SmartScanner implements Iterable<SmartFile> {

	private final MatchPatterns includesPatterns;
	private final MatchPatterns excludesPatterns;
	private final File basedir;
	private final int nThreads;

	public SmartScanner(File basedir, String[] includes, String[] excludes, int nThreads) {
		this.basedir = basedir;
		this.nThreads = nThreads;
		includesPatterns = MatchPatterns.from(ScannerTools.getIncludes(includes));
		excludesPatterns = MatchPatterns.from(ScannerTools.getExcludes(excludes));

	}

	public void scan(SmartFileReceiver smartFileReceiver) throws InterruptedException {
		MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, smartFileReceiver, nThreads);
		multiReader.scanThreaded();
		multiReader.awaitScanResult();
		multiReader.close();
	}

	@Override
	public Iterator<SmartFile> iterator() {
		try {
			final MultiReaderSingleWorker
			multiReaderSingleWorker = new MultiReaderSingleWorker(basedir, includesPatterns,
					excludesPatterns, nThreads);
			return new SmartFileIterator(multiReaderSingleWorker);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static class SmartFileIterator implements Iterator<SmartFile> {
		private final MultiReaderSingleWorker multiReaderSingleWorker;

		private SmartFileIterator(MultiReaderSingleWorker multiReaderSingleWorker) {
			this.multiReaderSingleWorker = multiReaderSingleWorker;
		}

		@Override
		public boolean hasNext() {
			return multiReaderSingleWorker.hasNext();
		}

		@Override
		public SmartFile next() {
			SmartFile next = multiReaderSingleWorker.next();
			if (next == null) {
				throw new NoSuchElementException("Illegal state");
			}
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not Supported");

		}
	}
}
