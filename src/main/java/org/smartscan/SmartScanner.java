package org.smartscan;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.Filters;
import org.smartscan.tools.MultiReader;
import org.smartscan.tools.MultiReaderSingleWorker;
import org.smartscan.tools.ScannerTools;

import java.io.File;
import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 */
public class SmartScanner implements Iterable<SmartFile> {

	private final Filters includesPatterns;
	private final Filters excludesPatterns;
	private final File basedir;
	private final int nThreads;

	public SmartScanner(File basedir, String[] includes, String[] excludes, int nThreads) {
		this.basedir = basedir;
		this.nThreads = nThreads;
		includesPatterns = Filters.from(ScannerTools.getIncludes(includes));
		excludesPatterns = Filters.from(ScannerTools.getExcludes(excludes));
	}

	public void scan(SmartFileReceiver smartFileReceiver) throws InterruptedException {
		MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, smartFileReceiver, nThreads);
		multiReader.beginThreadedScan();
		multiReader.awaitCompletion();
	}

	@Override
	public Iterator<SmartFile> iterator() {
		try {
			return new MultiReaderSingleWorker(basedir, includesPatterns, excludesPatterns, nThreads);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
