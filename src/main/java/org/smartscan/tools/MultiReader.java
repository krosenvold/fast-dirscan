package org.smartscan.tools;


import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Reads with multiple threads
 */
@SuppressWarnings("Since15")
public class MultiReader
        extends ModernBase {

    public static final char[][] NO_FILES_VPATH_ = new char[0][];
    public static final SmartFile[] NO_SMARTFILES = new SmartFile[0];

    private final ForkJoinPool executor;

    private final SmartFileReceiver smartFileReceiver;

    private final SmartFileReceiver dirReceiver;

    private final ScanCache scanCache;

    private final boolean followSymlinks;


    public MultiReader(File basedir, Filters includes, Filters excludes, SmartFileReceiver smartFileReceiver,
                       SmartFileReceiver dirListener, int nThreads, boolean followSymlinks1) {
        super(basedir, includes, excludes);
        ScannerTools.verifyBaseDir(basedir);
        executor = new ForkJoinPool(nThreads);
        this.smartFileReceiver = smartFileReceiver;
        this.dirReceiver = dirListener;
        scanCache = new ScanCache();
        followSymlinks = followSymlinks1;
    }

    public boolean isComplete() {
        final boolean quiescent = executor.isQuiescent();
        if (quiescent) {
            executor.shutdown();
        }
        return quiescent;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void awaitCompletion() {
        while (!executor.isQuiescent()) {
            doSleep(1);
        }
        executor.shutdown();
    }

    public void beginThreadedScan()
            throws IllegalStateException, InterruptedException {
        Runnable scanner = new Runnable() {
            @Override
            public void run() {
                scandir(Java7SmartFile.createSmartFile(basedir, NO_FILES_VPATH_), NO_FILES_VPATH_);
            }
        };
        executor.submit(scanner);
    }


    @SuppressWarnings("AssignmentToMethodParameter")
    private void scandir(SmartFile parent, char[][] unmodifyableparentvpath) {
        @Nullable SmartFile firstDir;
        @Nullable char[][] firstVpath;
        do {
            firstDir = null;
            firstVpath = null;
            char[][] mutablevpath = copyWithOneExtra(unmodifyableparentvpath);
            final int vpathIdx = mutablevpath.length - 1;
            SmartFile[] smartFiles = scanCache.createSmartFiles(parent, unmodifyableparentvpath);
            for (final SmartFile smartFile : smartFiles) {
                mutablevpath[vpathIdx] = smartFile.getFileNameChar();

                boolean shouldInclude = shouldInclude(mutablevpath);

                if (followSymlinks || !smartFile.isSymbolicLink()) {
                    if (smartFile.isFile()) {
                        if (shouldInclude) {
                            smartFileReceiver.accept(smartFile);
                        }
                    } else if (smartFile.isDirectory()) {
                        if (shouldInclude || couldHoldIncluded(mutablevpath)) {
                            if (followSymlinks || !smartFile.isSymbolicLink()) {
                                if (dirReceiver != null) dirReceiver.accept(smartFile);
                                if (firstDir == null) {
                                    firstDir = smartFile;
                                    firstVpath = copy(mutablevpath);
                                } else {
                                    final char[][] copy = copy(mutablevpath);
                                    new RecursiveAction() {
                                        @Override
                                        protected void compute() {
                                            scandir(smartFile, copy);
                                        }
                                    }.fork(); // Todo: fix swallowed exceptions
                                }
                            }
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