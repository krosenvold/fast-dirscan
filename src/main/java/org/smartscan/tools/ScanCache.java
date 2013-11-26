package org.smartscan.tools;

import org.smartscan.api.CachedJava7SmartFile;
import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

/**
 * @author Kristian Rosenvold
 */
public class ScanCache {

    public static final SmartFile[] NO_SMARTFILES = new SmartFile[0];

    private final File baseDir;
    private final Filters includes;
    private final Filters excludes;
    private final ConcurrentHashMap<SmartFile, SmartFile[]> cache;

    private ScanCache(File basedir, Filters includes, Filters excludes, ConcurrentHashMap<SmartFile, SmartFile[]> cache) {
        baseDir = basedir.getAbsoluteFile();
        this.includes = includes;
        this.excludes = excludes;
        this.cache = cache;
    }


    public static File getCacheBaseDir() {
        return new File("target");
    }

    static File getCacheFile(File baseDir) {
        return new File(getCacheBaseDir(), "testCache_" + baseDir.getPath().replace(File.separatorChar, '_'));
    }

    public ScanCache(File baseDir, ConcurrentHashMap<SmartFile, SmartFile[]> cache) {
        this(baseDir, null, null, cache);
    }


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
        if (smartFiles1 != null) {
            return smartFiles1;
        }
        final SmartFile[] smartFiles = createSmartFiles(dir.listFiles(), dirvpath);
        cache.put(dir, smartFiles);
        return smartFiles;
    }

    public void writeTo() throws IOException {
        final FileWriter fw = new FileWriter(getCacheFile(baseDir));
        fw.write(baseDir.getAbsolutePath());
        fw.write('\n');
        fw.write(includes != null ? Integer.toString(includes.hashCode()) : "0");
        fw.write('\n');
        fw.write(excludes != null ? Integer.toString(excludes.hashCode()) : "0");
        fw.write('\n');

        final SortedMap<SmartFile, SmartFile[]> sorted = new TreeMap<SmartFile, SmartFile[]>(cache);
        Callable<Object> loader = new Callable<Object>() {
            @Override
            public Object call() throws IOException, NumberFormatException {

                for (Map.Entry<SmartFile, SmartFile[]> smartFile : sorted.entrySet()) {
                    CachedFile.writeDir(fw, smartFile.getKey());
                    for (SmartFile smartFile1 : smartFile.getValue()) {
                        CachedFile.writeDirEntry(fw, smartFile1);
                    }
                }
                fw.close();
                return null;
            }
        };
        new Thread(new FutureTask<Object>(loader)).start();

    }

    private static Map<File, ScanCache> instances = new ConcurrentHashMap<File, ScanCache>();

    public static ScanCache mavenDefault(File basedir, Filters includes, Filters excludes) {
        ScanCache instance = instances.get( basedir);
        if (instance != null) return instance;

        File cacheFile = getCacheFile(basedir);
        if (cacheFile.exists()) try {
            return fromFile(cacheFile);
        } catch (IOException ignore) {
        }
        final ScanCache scanCache = new ScanCache(basedir, includes, excludes, new ConcurrentHashMap<SmartFile, SmartFile[]>());
        instances.put(basedir, scanCache);
        return scanCache;
    }

    public static ScanCache fromFile(File cacheStore1) throws IOException {
        FileReader fr = new FileReader(cacheStore1);
        final BufferedReader br = new BufferedReader(fr, 32768 * 4);

        final ConcurrentHashMap<SmartFile, SmartFile[]> cache = new ConcurrentHashMap<SmartFile, SmartFile[]>();

        final String basedir = br.readLine();
        String inclHsah = br.readLine();
        String exclHash = br.readLine();

        Callable<Object> loader = new Callable<Object>() {
            @Override
            public Object call() throws IOException, NumberFormatException {

                String line;
                SmartFile rootFile = null;
                List<SmartFile> kids = new ArrayList<SmartFile>();
                while ((line = br.readLine()) != null) {
                    if (line.charAt(0) == 'D') {
                        int fileIdx = line.indexOf(' ', 2);
                        final String substring = line.substring(1, fileIdx);
                        long timeStamp = Long.parseLong(substring, Character.MAX_RADIX);
                        String path = line.substring(fileIdx + 1);
                        if (rootFile != null) {
                            cache.put(rootFile, kids.toArray(new SmartFile[kids.size()]));
                            kids.clear();
                        }
                        int lastDir = path.lastIndexOf(File.separatorChar);
                        String name = lastDir >= 0 ? path.substring(lastDir + 1) : path;
                        String path1 = lastDir >= 0 ? path.substring(0, lastDir) : "";

                        rootFile = CachedJava7SmartFile.createCachedSmartDir(basedir, path1, name, timeStamp);
                    } else {
                        char type = line.charAt(1);
                        String fn = line.substring(3);
                        kids.add(CachedJava7SmartFile.createCachedSmartFile(basedir, rootFile.getVpath(), fn, type));
                    }
                }
                if (rootFile != null) {
                    cache.put(rootFile, kids.toArray(new SmartFile[kids.size()]));
                    kids.clear();
                }
                return null;
            }
        };
        new Thread(new FutureTask(loader)).start();


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
