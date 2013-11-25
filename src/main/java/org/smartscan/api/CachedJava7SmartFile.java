package org.smartscan.api;

import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public class CachedJava7SmartFile extends BaseJava7SmartFile implements SmartFile {
	private final Long lastModified;
	private final char type;

	private CachedJava7SmartFile(File basedir, String vpath, String fileName, Long lastModified, char type)
	{
		super(basedir, vpath, fileName);
		this.lastModified = lastModified;
		this.type = type;
	}

	public static SmartFile createCachedSmartFile(String basedir, String path, String fn, char type) {
		return new CachedJava7SmartFile(new File(basedir), path , fn, null, type);
	}

	public static SmartFile createCachedSmartDir(String basedir, String path, String fileName, long lastModified) {
		return new CachedJava7SmartFile(new File(basedir), path,  fileName, lastModified, 'D');
	}

	@Override
	public boolean isFile() {
		return 'F' == type;
	}

	@Override
	public boolean isDirectory() {
		return 'D' == type;
	}


	@Override
	public String toString() {
		return getVpath();
	}


	@Override
	public long lastModified(){
		return lastModified != null ? lastModified : file.lastModified();
	}
}
