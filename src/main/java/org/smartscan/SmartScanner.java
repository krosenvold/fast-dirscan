package org.smartscan;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.MatchPatterns;
import org.smartscan.tools.MultiReader;
import org.smartscan.tools.MultiReaderSingleWorker;
import org.smartscan.tools.ScannerTools;

import java.io.File;
import java.util.Iterator;

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
