package org.smartscan;

import org.codehaus.plexus.util.AbstractScanner;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;
import org.smartscan.tools.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 */
public class SmartScanner {

	private @Nonnull
    Filters includesPatterns;
	private @Nonnull Filters excludesPatterns;
	private File basedir;
	private int nThreads;
    private SmartFileReceiver dirListener;
    private boolean caseSensitive;
    private boolean followSymlinks = true;

    public SmartScanner(File basedir, String[] includes, String[] excludes, int nThreads) {
		this.basedir = basedir;
		this.nThreads = nThreads;
		includesPatterns = Filters.from(caseSensitive, ScannerTools.getIncludes(includes));
		excludesPatterns = Filters.from(caseSensitive, ScannerTools.getExcludes(excludes));
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
        MultiReader multiReader = new MultiReader(basedir, includesPatterns, excludesPatterns, fileReceiver, dirListener, nThreads, followSymlinks);
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
		excludesPatterns = Filters.join(excludesPatterns, Filters.from(caseSensitive, AbstractScanner.DEFAULTEXCLUDES));
	}

	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	public void setExcludes(String excludes){
		this.excludesPatterns = Filters.from(caseSensitive, excludes);
	}

	public void setIncludes(String includes){
		this.includesPatterns = Filters.from(caseSensitive, includes);
	}

	public void setFollowSymlinks(){
        this.followSymlinks = true;

	}

	public void setCaseSensitive(boolean caseSensitive){
        this.caseSensitive = caseSensitive;
        this.excludesPatterns = excludesPatterns.changeCase(caseSensitive);
        this.includesPatterns = includesPatterns.changeCase(caseSensitive);
	}

	public void setDirectoryListener(SmartFileReceiver smartFileReceiver){
        this.dirListener = smartFileReceiver;
	}
}
