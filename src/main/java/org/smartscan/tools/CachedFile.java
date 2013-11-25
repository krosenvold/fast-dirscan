package org.smartscan.tools;

import org.smartscan.api.SmartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * @author Kristian Rosenvold
 */
public class CachedFile {
	private final String basedir;
	private final String vpath;
	private final long lastModified;
	private final boolean isFile;
	private final boolean isDirectory;

	public CachedFile(String basedir, String vpath, long lastModified, boolean isFile, boolean isDirectory) {
		this.basedir = basedir;
		this.vpath = vpath;
		this.lastModified = lastModified;
		this.isFile = isFile;
		this.isDirectory = isDirectory;
	}

	public static void writeDir(Writer os, SmartFile smartFile) throws IOException {
		writeFileType(os, smartFile);
		os.write(Long.toString(smartFile.lastModified()));
		 os.write((byte)' ');
		writePath(os, smartFile);
		os.write('\n');
	}

	public static void writeDirEntry(Writer os, SmartFile smartFile) throws IOException {
		os.write((byte) ' ');
		writeFileType(os, smartFile);
		os.write((byte)' ');
		os.write(smartFile.getFileNameChar());
		os.write('\n');
	}

	private static void writePath(Writer os, SmartFile smartFile) throws IOException {
		for (char[] chars : smartFile.getParentVpath()) {
			os.write(chars);
			os.write(File.separatorChar);
		}
		os.write(smartFile.getFileNameChar());
	}

	private static void writeFileType(Writer os, SmartFile smartFile) throws IOException {
		if (smartFile.isFile()) os.write((byte)'F');
		else if (smartFile.isDirectory()) os.write((byte)'D');
		else os.write((byte)'?');
	}

}
