package org.smartscan.tools;

import org.smartscan.api.CachedJava7SmartFile;
import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kristian Rosenvold
 */
public class ScanCache {

	public static final SmartFile[] NO_SMARTFILES = new SmartFile[0];

	private final File baseDir;
	private final Filters includes;
	private final Filters excludes;
	private final File cacheBaseDir;

	public ScanCache(File basedir, Filters includes, Filters excludes, ConcurrentHashMap<SmartFile,SmartFile[]> cache) {
		this( getCacheBaseDir(), basedir, includes, excludes, cache);
	}


	public static File getCacheBaseDir() {
		return new File("target");
	}

	 static File getCacheFile(File baseDir) {
		return new File(getCacheBaseDir(), "testCache_" + baseDir.getPath().replace(File.separatorChar, '_'));
	}

	public ScanCache(File baseDir, ConcurrentHashMap<SmartFile,SmartFile[]> cache) {
		this(getCacheBaseDir(), baseDir, null, null, cache);
	}

	private ScanCache(File cacheBaseDir, File basedir, Filters includes, Filters excludes, ConcurrentHashMap<SmartFile,SmartFile[]> cache) {
		this.cacheBaseDir = cacheBaseDir;
		this.baseDir = basedir.getAbsoluteFile();
		this.includes = includes;
		this.excludes = excludes;
		this.cache = cache;
	}

	private final ConcurrentHashMap<SmartFile,SmartFile[]> cache;

	private static SmartFile[] createSmartFiles(File[] files, char[][] unmodifyableparentvpath) {

		if (files == null) {
			return NO_SMARTFILES;
		}

		final int length = files.length;
		SmartFile[] result = new SmartFile[length];

		for (int i = 0; i < length; i++) {
			result[i] = Java7SmartFile.createSmartFile(files[i], unmodifyableparentvpath);
		}
		return result;
	}
	public SmartFile[] createSmartFiles(SmartFile dir, char[][] dirvpath) {
		final SmartFile[] smartFiles1 = cache.get(dir);
		if (smartFiles1 != null){
			return smartFiles1;
		}
		final SmartFile[] smartFiles = createSmartFiles(dir.listFiles(), dirvpath);
		cache.put( dir, smartFiles);
		return smartFiles;
	}

	public void writeTo() throws IOException {
		FileWriter fw = new FileWriter(getCacheFile(baseDir));
		fw.write(baseDir.getAbsolutePath());fw.write('\n');
		fw.write(includes != null ? Integer.toString(includes.hashCode()) : "0");fw.write('\n');
		fw.write(excludes != null ? Integer.toString(excludes.hashCode()): "0");fw.write('\n');

		for (Map.Entry<SmartFile, SmartFile[]> smartFile : cache.entrySet()) {
			CachedFile.writeDir(fw, smartFile.getKey());
			for (SmartFile smartFile1 : smartFile.getValue()) {
				CachedFile.writeDirEntry(fw, smartFile1);
			}
		}
		fw.close();

	}

	public static ScanCache mavenDefault(File basedir, Filters includes, Filters excludes) {
		File cacheFile = getCacheFile(basedir);
		if (cacheFile.exists()) try {
			return fromFile(cacheFile);
		} catch (IOException ignore) {
		}
		return new ScanCache(getCacheBaseDir(), basedir, includes, excludes, new ConcurrentHashMap<SmartFile, SmartFile[]>());
	}

	public static ScanCache fromFile(File cacheStore1) throws IOException {
		FileReader fr = new FileReader(cacheStore1);
		BufferedReader br = new BufferedReader(fr);

		ConcurrentHashMap<SmartFile,SmartFile[]> cache = new ConcurrentHashMap<SmartFile,SmartFile[]>();

		String basedir = br.readLine();
		String inclHsah = br.readLine();
		String exclHash = br.readLine();
		String line;
		SmartFile rootFile = null;
		List<SmartFile> kids = new ArrayList<SmartFile>();
		while ((line = br.readLine()) != null){
			if (line.charAt(0) == 'D'){
				int fileIdx = line.indexOf(' ', 2);
				final String substring = line.substring(1, fileIdx);
				long timeStamp = Long.parseLong(substring);
				String path = line.substring( fileIdx +1);
				if (rootFile != null){
					cache.put( rootFile, kids.toArray(new SmartFile[kids.size()]));
					kids.clear();
				}
				int lastDir = path.lastIndexOf(File.separatorChar);
				String name = lastDir >= 0 ? path.substring( lastDir + 1): path;
				String path1 = lastDir >= 0 ? path.substring(0, lastDir) : "";

				rootFile = CachedJava7SmartFile.createCachedSmartDir(basedir, path1, name, timeStamp);
			} else {
			 char type = line.charAt(1);
				String fn = line.substring(3);
				kids.add(CachedJava7SmartFile.createCachedSmartFile(basedir, rootFile.getVpath(), fn, type));
			}
		}
		if (rootFile != null){
			cache.put( rootFile, kids.toArray(new SmartFile[kids.size()]));
			kids.clear();
		}
		return new ScanCache(new File(basedir), cache);
	}

	public void close() {
		try {
			writeTo();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
