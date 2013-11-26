package org.smartscan;

import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.*;

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
    private final ScanCache scanCache;

	public SmartScanner(File basedir, String[] includes, String[] excludes, int nThreads) {
		this.basedir = basedir;
		this.nThreads = nThreads;
		includesPatterns = Filters.from(ScannerTools.getIncludes(includes));
		excludesPatterns = Filters.from(ScannerTools.getExcludes(excludes));
        scanCache = ScanCache.mavenDefault(basedir, includesPatterns, excludesPatterns);
	}

    public SmartScanner(File basedir, Filters includes, Filters excludes, int nThreads, ScanCache scanCache) {
        this.basedir = basedir;
        this.nThreads = nThreads;
        includesPatterns = includes;
        excludesPatterns = excludes;
        this.scanCache = scanCache;
    }

    public void scan2(SmartFileReceiver smartFileReceiver) throws InterruptedException {
        MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, smartFileReceiver, nThreads, scanCache);
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
