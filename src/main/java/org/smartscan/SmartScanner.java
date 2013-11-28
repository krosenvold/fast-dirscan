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
    private SmartFileReceiver dirListener;

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


    public void scan(SmartFileReceiver fileReceiver) throws InterruptedException {
		MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, fileReceiver, dirListener, nThreads, true);
		multiReader.beginThreadedScan();
		multiReader.awaitCompletion();
	}

    public void scanReference(SmartFileReceiver fileReceiver) throws InterruptedException {
        ReferenceMultiReader multiReader = new ReferenceMultiReader(basedir, includesPatterns, excludesPatterns, fileReceiver, nThreads);
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
        this.dirListener = smartFileReceiver;
	}


	// And also: getIncludedDirectories
}
