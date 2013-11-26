package org.smartscan.tools;


import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nullable;
import java.io.File;

/**
 * Reads with multiple threads
 */
@SuppressWarnings({"Since15", "UnnecessaryFullyQualifiedName"})
public class MultiReader
		extends ModernBase {

	public static final char[][] NO_FILES_VPATH_ = new char[0][];

    private final java.util.concurrent.ForkJoinPool executor;

	private final SmartFileReceiver smartFileReceiver;

	private final ScanCache scanCache;

	public MultiReader(File basedir, Filters includes, Filters excludes, SmartFileReceiver smartFileReceiver,
                       int nThreads, ScanCache scanCache1) {
		super(basedir, includes, excludes);
		ScannerTools.verifyBaseDir(basedir);
		executor = new java.util.concurrent.ForkJoinPool(nThreads);
		this.smartFileReceiver = smartFileReceiver;
		scanCache = scanCache1;
	}

	public boolean isComplete() {
		final boolean quiescent = executor.isQuiescent();
		if (quiescent) {
			executor.shutdown();
			scanCache.close();
		}
		return quiescent;
	}

	@SuppressWarnings("StatementWithEmptyBody")
	public void awaitCompletion() {
		while (!executor.isQuiescent()) {
			doSleep(1);
		}
		executor.shutdown();
		scanCache.close();
	}

	public void beginThreadedScan()
			throws IllegalStateException, InterruptedException {
		Runnable scanner = new Runnable() {
			@Override
			public void run() {
				scandir(Java7SmartFile.createRootDir(basedir), NO_FILES_VPATH_);
			}
		};
		executor.submit(scanner);
	}


	@SuppressWarnings("AssignmentToMethodParameter")
	private void scandir(SmartFile parent, char[][] unmodifyableparentvpath) {
		@Nullable SmartFile firstDir;
		@Nullable char[][] firstVpath;
		do {
			firstDir = null; firstVpath = null;
			char[][] mutablevpath = copyWithOneExtra(unmodifyableparentvpath);
			final int vpathIdx = mutablevpath.length - 1;
			SmartFile[] smartFiles = scanCache.createSmartFiles(parent, unmodifyableparentvpath);
			for (final SmartFile smartFile : smartFiles) {
				mutablevpath[vpathIdx] = smartFile.getFileNameChar();

				boolean shouldInclude = shouldInclude(mutablevpath);

				if (smartFile.isFile()) {
					if (shouldInclude) {
						smartFileReceiver.accept(smartFile);
					}
				} else if (smartFile.isDirectory()) {
					if (shouldInclude || couldHoldIncluded(mutablevpath)) {
						if (firstDir == null) {
							firstDir = smartFile;
							firstVpath = copy(mutablevpath);
						} else {
							final char[][] copy = copy(mutablevpath);
							new java.util.concurrent.RecursiveAction() {
								@Override
								protected void compute() {
									scandir(smartFile, copy);
								}
							}.fork(); // Todo: fix swallowed exceptions
						}
					}
				}
			}
			parent = firstDir;
			unmodifyableparentvpath = firstVpath;
		}
		while (firstDir != null);
	}

}