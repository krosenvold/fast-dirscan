package org.smartscan.api;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import static junit.framework.Assert.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class Java7SmartFileTest {
    @Test
    public void symLinkToDir() throws Exception {
        File basedir = new File("src/test/resources/symlink-test/toD3");
        final SmartFile smartFile = Java7SmartFile.createSmartFile(basedir, new char[0][]);
        assertTrue( smartFile.isSymbolicLink());
    }

    @Test
    public void symLinkToFile() throws Exception {
        File basedir = new File("src/test/resources/symlink-test/toFile4");
        basedir = basedir.getAbsoluteFile();
        final BasicFileAttributes basicFileAttributes = Files.readAttributes(basedir.toPath(), BasicFileAttributes.class);
        assertTrue( basicFileAttributes.isSymbolicLink());
    }
}
