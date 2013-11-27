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

	public void addDefaultExcludes()
	{
		excludesPatterns = Filters.join(excludesPatterns, Filters.from(AbstractScanner.DEFAULTEXCLUDES));
	}

	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	public void setExcludes(String excludes){
		this.excludesPatterns = Filters.from( excludes);
	}

	public void setIncludes(String includes){
		this.includesPatterns = Filters.from( includes);
	}

	public void setFollowSymlinks(){

	}

	public void setCaseSensitive(){

	}

	public void setDirectoryListener(SmartFileReceiver smartFileReceiver){

	}


	// And also: getIncludedDirectories
}
