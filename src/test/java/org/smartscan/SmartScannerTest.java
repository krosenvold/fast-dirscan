package org.smartscan;

import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Test;
import org.smartscan.api.SmartFile;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class SmartScannerTest {

	@Test
	public void shouldShowCorrectElements(){
		File basedir = new File("src/test/resources/dir-layout-copy");
		SmartScanner ss = new SmartScanner(basedir, null,null ,2);
		int cnt = 0;
		for (SmartFile sf : ss) {
			cnt++;
			System.out.println("ss.getFile() = " + sf.getFile().getPath());
		}
		assertEquals(3, cnt);

		getReference(basedir);
	}

	@Test
	public void shouldShowCorrectElementsOtherDir(){
		File basedir = new File("src/test/resources/directorywalker");
		String[] reference = getReference(basedir);

		SmartScanner ss = new SmartScanner(basedir, null,null ,2);
		int cnt = 0;
		for (SmartFile sf : ss) {
			cnt++;
			System.out.println("ss.getFile() = " + sf.getFile().getPath());
		}
		assertEquals(reference.length, cnt);

	}

	private String[] getReference(File basedir) {
		DirectoryScanner ds = new DirectoryScanner();
		ds.setBasedir(basedir);
		ds.scan();
		String[] includedFiles = ds.getIncludedFiles();
		for (String includedFile : includedFiles) {
			System.out.println("includedFile = " + includedFile);

		}
		return includedFiles;
	}
}