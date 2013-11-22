package org.smartscan.tools;

import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;

import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public class ScanCache {

	public static final SmartFile[] NO_SMARTFILES = new SmartFile[0];


	public static SmartFile[] createSmartFiles(File[] files, char[][] unmodifyableparentvpath) {

		if (files == null) return NO_SMARTFILES;

		final int length = files.length;
		SmartFile[] result = new SmartFile[length];

		for (int i = 0; i < length; i++) {
			result[i] = Java7SmartFile.createSmartFile(files[i], unmodifyableparentvpath);
		}
		return result;
	}
	public SmartFile[] createSmartFiles(SmartFile dir, char[][] unmodifyableparentvpath) {
		return createSmartFiles(dir.listFiles(), unmodifyableparentvpath);
	}

	SmartFile[] getCachedFiles(SmartFile dir){
		return createSmartFiles(dir, dir.getParentVpath());
	}
}
