package org.smartscan.api;

import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public interface SmartFile extends Comparable {

    boolean isFile();

    boolean isDirectory();

    File getFile();

    String getVpath();

	File[] listFiles();

	char[] getFileNameChar();
}
