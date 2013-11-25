package org.smartscan.api;

import org.smartscan.tools.Filter;
import org.smartscan.tools.ModernBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 * @author Kristian Rosenvold
 */
public class BaseJava7SmartFile {

	protected final File file;
	protected final char[][] parentVpath;
	protected final char[] fileNameChar;

	protected BaseJava7SmartFile(File basedir, String vpath, String fileName)
	{

		file = new File( basedir, vpath + File.separatorChar + fileName);
		parentVpath = Filter.tokenizePathToCharArray(vpath, File.separatorChar);
		fileNameChar = fileName.toCharArray();
	}

	protected BaseJava7SmartFile(File file, char[][] parentVpath, char[] fileNameChar) {
		this.file = file;
		this.parentVpath = parentVpath;
		this.fileNameChar = fileNameChar;
	}
	public String getPath() {
		return file.getPath();
	}

	public File getFile()
	{
		return file;
	}



	public String getVpath(){
		StringBuilder result = new StringBuilder();
		for (char[] chars : parentVpath) {
			result.append(chars);
			result.append(File.separatorChar);
		}
		result.append( fileNameChar);
		return result.toString();
	}

	public char[][] getParentVpath() {
		return parentVpath;
	}

	public char[] getFileNameChar() {
		return fileNameChar;
	}

	public char[][] getVPathOfThis() {
		final char[][] chars = ModernBase.copyWithOneExtra(parentVpath);
		chars[ parentVpath.length] = fileNameChar;
		return chars;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SmartFile)) return false;

		SmartFile that = (SmartFile) o;

		if (!Arrays.equals(fileNameChar, that.getFileNameChar())) return false;
		if (parentVpath.length != that.getParentVpath().length) return false;
		for (int i = 0; i < parentVpath.length; i++){
			if (!Arrays.equals(parentVpath[i], that.getParentVpath()[i])) return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode += Arrays.hashCode(fileNameChar);
		for (char[] chars : parentVpath) {
			hashCode += Arrays.hashCode(chars);
		}
		return hashCode;
	}

	protected static BasicFileAttributes getBasicFileAttributes(File file) {
		BasicFileAttributes basicFileAttributes;
		try
		{
			basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		}
		catch ( IOException e )
		{
			throw new RuntimeException(e);
		}
		return basicFileAttributes;
	}

	public int compareTo(SmartFile o) {
		return getPath().compareTo( o.getPath());
	}


	@Override
	public String toString() {
		return getVpath();
	}

	public File[] listFiles() {
		return file.listFiles();
	}


}

