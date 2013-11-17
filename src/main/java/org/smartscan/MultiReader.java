package org.smartscan;


import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.tools.ScannerTools;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class MultiReader
		extends ModernBase {

	public static final char[][] NO_FILES_VPATH_ = new char[0][];

	private final AtomicInteger threadsStarted = new AtomicInteger(1);

	private final ForkJoinPool executor;

	private final FastFileReceiver fastFileReceiver;

	public final AtomicBoolean completed = new AtomicBoolean(false);

	private static final String[] NOFILES = new String[0];
	private final FileSystem fileSystem = FileSystems.getDefault();
	private final int nThreads;

	/**
	 * Sole constructor.
	 *
	 * @noinspection JavaDoc
	 */
	public MultiReader(File basedir, String[] includes, String[] excludes, FastFileReceiver fastFileReceiver,
					   int nThreads) {
		super(basedir, includes, excludes);
		ScannerTools.verifyBaseDir(basedir);
		this.nThreads = nThreads;
		executor = new ForkJoinPool(nThreads);
		this.fastFileReceiver = fastFileReceiver;
	}


	public void awaitScanResult() {
		//noinspection StatementWithEmptyBody
		while (threadsStarted.get() > 0) {
		}
		executor.shutdown();
	}


	public void scanThreaded()
			throws IllegalStateException, InterruptedException {
		Runnable scanner = new Runnable() {
			@Override
			public void run() {
				asynchscandir(basedir, NO_FILES_VPATH_);
			}
		};
		executor.submit(scanner);
	}

	private void asynchscandir(File dir, char[][] vpath) {
		scandir(dir, vpath);
		int i = threadsStarted.decrementAndGet();
		if (i == 0) {
			completed.set(true);
		}

	}


	@SuppressWarnings("AssignmentToMethodParameter")
	private void scandir(File parent, char[][] unmodifyableparentvpath) {
		@Nullable File firstDir;
		@Nullable char[][] firstVpath;
		firstDir = null;
		firstVpath = null;

		File[] newfiles = parent.listFiles();

		if (newfiles != null) {

			char[][] mutablevpath = copyWithOneExtra(unmodifyableparentvpath);
			for (File file : newfiles) {
				mutablevpath[mutablevpath.length - 1] = file.getName().toCharArray();
				BasicFileAttributes basicFileAttributes;
				try {
					Path path = fileSystem.getPath(file.getPath());
					basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				boolean shouldInclude = shouldInclude(mutablevpath);
				if (basicFileAttributes.isRegularFile()) {
					if (shouldInclude) {
						fastFileReceiver.accept(new FastFile(file, unmodifyableparentvpath));
					}
				} else if (basicFileAttributes.isDirectory()) {
					if (shouldInclude || couldHoldIncluded(mutablevpath)) {
						if (firstDir == null) {
							firstDir = file;
							firstVpath = copy(mutablevpath);
						} else {
							if (executor.getQueuedSubmissionCount() > (nThreads/2)) {
								scandir(file, copy(mutablevpath));
							} else {
								final AsynchScanner target = new AsynchScanner(file, copy(mutablevpath));
								threadsStarted.incrementAndGet();
								//  System.out.println( "executor = " + executor );
								target.fork();
							}
						}
					}
				}
			}
			if (firstDir != null) {
				scandir(firstDir, firstVpath);
			}
		}
	}


	class AsynchScanner extends ForkJoinTask {
		File dir;

		char[][] vpath;

		AsynchScanner(File dir, char[][] vpath) {
			this.dir = dir;
			this.vpath = vpath;
		}


		@Override
		public Object getRawResult() {
			return true;
		}

		@Override
		protected void setRawResult(Object value) {

		}

		@Override
		protected boolean exec() {
			try {

				asynchscandir(dir, vpath);
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public void close() {
		executor.shutdown();
	}

}