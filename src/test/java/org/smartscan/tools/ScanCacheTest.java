package org.smartscan.tools;

import org.junit.Test;
import org.smartscan.api.Java7SmartFile;
import org.smartscan.api.SmartFile;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * @author Kristian Rosenvold
 */
public class ScanCacheTest {

	private File baseDir;

	@Test
	public void testCreateSmartFiles() throws Exception {
		baseDir = new File("src/test/resources");
		ScanCache sc = ScanCache.mavenDefault(baseDir, Filters.from(), Filters.from());
		SmartFile sf = Java7SmartFile.createSmartFile(new File("src/test/resources/dir-layout-copy"), new char[][]{"src".toCharArray(), "test".toCharArray(), "resources".toCharArray()});
		sc.createSmartFiles(sf, sf.getVPathOfThis());
		SmartFile sf2 = Java7SmartFile.createSmartFile(new File("src/test/resources/directorywalker"), new char[][]{"src".toCharArray(), "test".toCharArray(), "resources".toCharArray()});
		sc.createSmartFiles(sf2, sf2.getVPathOfThis());
		sc.writeTo();

		// WTF: Why getAbsoluteFile ??
		ScanCache sc3 = ScanCache.fromFile(ScanCache.getCacheFile(baseDir.getAbsoluteFile()));
		assertNotNull( sc3);


	}
}
