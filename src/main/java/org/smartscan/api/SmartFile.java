package org.smartscan.api;

import java.io.File;

/**
 * @author Kristian Rosenvold
 */
public interface SmartFile extends Comparable {

    /**
     * Indicates if this smartfile is a file. This may be a regular file or a symlink pointing
     * to a file. To determine in particular if this file is a symlink, call #isSymbolicLink
     * @return
     */
    boolean isFile();

    /**
     * Indicates if this smartfile is a directory-like thing that will respond to a listFiles call.
     * Note that this method also returns true for symlinks that point to actual directories.
     * To determine if this directory is actually a symlink pointing to a directory, call #isSymbolicLink
     * @return true if the smartfile is a directory or a symlink pointing to a directory
     */
    boolean isDirectory();

    /**
     * Indicates that this element is a symbolic link
     * @return true if this smartfile is a symbolic link, either to a file or a directory
     */
    boolean isSymbolicLink();

    File getFile();

    String getVpath();

	File[] listFiles();

	char[] getFileNameChar();

	char[][] getParentVpath();
}
