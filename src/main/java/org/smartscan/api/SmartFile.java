package org.smartscan.api;

import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public interface SmartFile extends Comparable<SmartFile> {

    boolean isFile();

    boolean isDirectory();

    File getFile();

    String getVpath();

	File[] listFiles();

	char[] getFileNameChar();

	char[][] getParentVpath();

	char[][] getVPathOfThis();

	long lastModified();

	String getPath();

}
