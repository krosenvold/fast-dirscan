package org.smartscan.api;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * A few interesting observations about symlinks
 * @author Kristian Rosenvold
 */
public class Java7SmartFileTest {
    @Test
    public void symLinkToDir() throws Exception {
        File symLinkToDir = new File("src/test/resources/symlink-test/toD3");
        final SmartFile smartFile = Java7SmartFile.createSmartFile(symLinkToDir, new char[0][]);
        assertTrue(symLinkToDir.isDirectory()); // Note; describes symlink target
        assertFalse( smartFile.isDirectory()); // And YESS !
        assertTrue(smartFile.isSymbolicLink());
    }

    @Test
    public void symLinkToFile() throws Exception {
        File symLinktoFile= new File("src/test/resources/symlink-test/toFile4");
        symLinktoFile = symLinktoFile.getAbsoluteFile();
        final BasicFileAttributes basicFileAttributes = Files.readAttributes(symLinktoFile.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        assertTrue(symLinktoFile.isFile()); // Describes symlink target
        assertFalse(basicFileAttributes.isRegularFile()); // And YESS !
        assertTrue(basicFileAttributes.isSymbolicLink());
    }
}
