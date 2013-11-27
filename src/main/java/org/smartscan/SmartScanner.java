package org.smartscan;

import org.codehaus.plexus.util.AbstractScanner;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.*;

import java.io.File;
import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 */
public class SmartScanner {

	private Filters includesPatterns;
	private Filters excludesPatterns;
	private File basedir;
	private int nThreads;

	public SmartScanner(File basedir, String[] includes, String[] excludes, int nThreads) {
		this.basedir = basedir;
		this.nThreads = nThreads;
		includesPatterns = Filters.from(ScannerTools.getIncludes(includes));
		excludesPatterns = Filters.from(ScannerTools.getExcludes(excludes));
	}

	public Iterable<SmartFile> scan() {
		return new Iterable<SmartFile>() {
			@Override
			public Iterator<SmartFile> iterator() {
				try {
					return new MultiReaderSingleWorker(basedir, includesPatterns, excludesPatterns, nThreads);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}


	public void scan(SmartFileReceiver smartFileReceiver) throws InterruptedException {
		MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, smartFileReceiver, nThreads);
		multiReader.beginThreadedScan();
		multiReader.awaitCompletion();
	}

	public void scan2(SmartFileReceiver smartFileReceiver) throws InterruptedException {
		CachingMultiReader multiReader = new CachingMultiReader(basedir, includesPatterns, excludesPatterns, smartFileReceiver, nThreads);
		multiReader.beginThreadedScan();
		multiReader.awaitCompletion();
	}

	/**
	 * Adds default exclusions to the current exclusions set.
	 */
	public void addDefaultExcludes()
	{
		excludesPatterns = Filters.join(excludesPatterns, Filters.from(AbstractScanner.DEFAULTEXCLUDES));
	}


	/*

	        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        scanner.setExcludes( getExcludes() );
        scanner.addDefaultExcludes();

        scanner.setIncludes( getIncludes() );

        scanner.scan();

        return scanner.getIncludedFiles();

		scanner.getIncludedDirectories()

		scanner.setFollowSymlinks( false );


Javdoc:
        final DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( javadocOutputDirectory );
        ds.setCaseSensitive( false );
        ds.setIncludes( new String[]{ } );
			ds.addDefaultExcludes();
	ds.scan();

	 */

}
