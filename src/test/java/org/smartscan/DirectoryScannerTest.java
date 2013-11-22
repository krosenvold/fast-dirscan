package org.smartscan;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DirectoryScannerTest {
    private static final String[] NONE = new String[0];

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private void createTestData()
            throws IOException {
        File rootDir = tempFolder.getRoot();
        File folder1 = new File(rootDir, "folder1");
        folder1.mkdirs();

        FileTestHelper.generateTestFile(new File(rootDir, "file1.txt"), 11);
        FileTestHelper.generateTestFile(new File(rootDir, "file2.txt"), 12);
        FileTestHelper.generateTestFile(new File(rootDir, "file3.dat"), 13);

        FileTestHelper.generateTestFile(new File(folder1, "file4.txt"), 14);
        FileTestHelper.generateTestFile(new File(folder1, "file5.dat"), 15);

       // File folder2 = new File(folder1, "ignorefolder");
        //folder2.mkdirs();
        // FileTestHelper.generateTestFile(new File(folder2, "file7.txt"), 17);
    }

    @Test
    public void testSimpleScan()
            throws Exception {
        createTestData();

        fitScanTest(true, true, true,
                /* includes */        null,
                /* excludes */        null,
                /* expInclFiles */    new String[]{"file1.txt", "file2.txt", "file3.dat", "folder1/file4.txt", "folder1/file5.dat"},
                /* expInclDirs */     new String[]{"", "folder1"},
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expNotExclFiles */ NONE,
                /* expNotExclDirs  */ NONE);

        // same without followSymlinks
        fitScanTest(true, false, true,
                /* includes */        null,
                /* excludes */        null,
                /* expInclFiles */    new String[]{"file1.txt", "file2.txt", "file3.dat", "folder1/file4.txt", "folder1/file5.dat"},
                /* expInclDirs */     new String[]{"", "folder1"},
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expNotExclFiles */ NONE,
                /* expNotExclDirs  */ NONE);
    }

    @Test
    public void testSimpleIncludes()
            throws Exception {
        createTestData();

        fitScanTest(true, true, true,
                /* includes        */ new String[]{"**/*.dat", "*.somethingelse"},
                /* excludes        */ null,
                /* expInclFiles    */ new String[]{"file3.dat", "folder1/file5.dat"},
                /* expInclDirs     */ NONE,
                /* expNotInclFiles */ new String[]{"file1.txt", "file2.txt", "folder1/file4.txt"},
                /* expNotInclDirs  */ new String[]{"", "folder1"},
                /* expExclFiles    */ NONE,
                /* expExclDirs     */ NONE);

        // same without followSymlinks
        fitScanTest(true, false, true,
                /* includes        */ new String[]{"**/*.dat", "*.somethingelse"},
                /* excludes        */ null,
                /* expInclFiles    */ new String[]{"file3.dat", "folder1/file5.dat"},
                /* expInclDirs     */ NONE,
                /* expNotInclFiles */ new String[]{"file1.txt", "file2.txt", "folder1/file4.txt"},
                /* expNotInclDirs  */ new String[]{"", "folder1"},
                /* expExclFiles    */ NONE,
                /* expExclDirs     */ NONE);
    }

    @Test
    public void testSimpleExcludes()
            throws Exception {
        createTestData();

        fitScanTest(true, true, true,
                /* includes        */ null,
                /* excludes        */ new String[]{"**/*.dat", "*.somethingelse"},
                /* expInclFiles    */ new String[]{"file1.txt", "file2.txt", "folder1/file4.txt"},
                /* expInclDirs     */ new String[]{"", "folder1"},
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expExclFiles    */ new String[]{"file3.dat", "folder1/file5.dat"},
                /* expExclDirs     */ NONE);

        // same without followSymlinks
        fitScanTest(true, false, true,
                /* includes        */ null,
                /* excludes        */ new String[]{"**/*.dat", "*.somethingelse"},
                /* expInclFiles    */ new String[]{"file1.txt", "file2.txt", "folder1/file4.txt"},
                /* expInclDirs     */ new String[]{"", "folder1"},
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expExclFiles    */ new String[]{"file3.dat", "folder1/file5.dat"},
                /* expExclDirs     */ NONE);
    }

  /*  public void testIsSymLin()
        throws IOException
    {
        File file = new File( "." );
        DirectoryScanner ds = new DirectoryScanner();
        ds.isSymbolicLink( file, "abc" );
    }
    */

    /**
     * Performs a scan and test for the given parameters if not null.
     */
    private void fitScanTest(boolean caseSensitive,
                             boolean followSymLinks,
                             boolean addDefaultExcludes,
                             String[] includes, String[] excludes,
                             String[] expectedIncludedFiles,
                             String[] expectedIncludedDirectories,
                             String[] expectedNotIncludedFiles,
                             String[] expectedNotIncludedDirectories,
                             String[] expectedExcludedFiles,
                             String[] expectedExcludedDirectories) throws InterruptedException {
        SmartScanner ds = new SmartScanner(tempFolder.getRoot(), includes, excludes, 5);

        // todo: Implement
        //   ds.setCaseSensitive( caseSensitive );
        //  ds.setFollowSymlinks( followSymLinks );

        //  if ( addDefaultExcludes )
        //   {
        //          ds.addDefaultExcludes();
        //    }


        SimpleListReceiver receiver = new SimpleListReceiver();
        ds.scan(receiver);

        checkFiles("expectedIncludedFiles", expectedIncludedFiles, receiver.getResult());
        //     checkFiles( "expectedIncludedDirectories", expectedIncludedDirectories, ds.getIncludedDirectories() );
        /*
        checkFiles( "expectedNotIncludedFiles", expectedNotIncludedFiles, ds.getNotIncludedFiles() );
        checkFiles( "expectedNotIncludedDirectories", expectedNotIncludedDirectories, ds.getNotIncludedDirectories() );
        checkFiles( "expectedExcludedFiles", expectedExcludedFiles, ds.getExcludedFiles() );
        checkFiles( "expectedExcludedDirectories", expectedExcludedDirectories, ds.getExcludedDirectories() );
        */
    }

    /**
     * Check if the resolved files match the rules of the expected files.
     *
     * @param expectedFiles
     * @param resolvedFiles
     */
    private void checkFiles(String category, String[] expectedFiles, String[] resolvedFiles) {
        if (expectedFiles != null) {
            String msg = category + " expected: " + Arrays.toString(expectedFiles) + " but got: " + Arrays.toString(resolvedFiles);
            Assert.assertNotNull(msg, resolvedFiles);
            Assert.assertEquals(msg, expectedFiles.length, resolvedFiles.length);

            Arrays.sort(expectedFiles);
            Arrays.sort(resolvedFiles);

            for (int i = 0; i < resolvedFiles.length; i++) {
                Assert.assertEquals(msg, expectedFiles[i], resolvedFiles[i].replace("\\", "/"));
            }
        }
    }

    private void checkFiles(String category, String[] expectedFiles, List<SmartFile> resolvedFiles) {
        if (expectedFiles != null) {
            String msg = category + " expected: " + Arrays.toString(expectedFiles) + " but got: " + Arrays.toString(resolvedFiles.toArray());
            Assert.assertNotNull(msg, resolvedFiles);
            Assert.assertEquals(msg, expectedFiles.length, resolvedFiles.size());

            Arrays.sort(expectedFiles);
            Collections.sort(resolvedFiles);

            for (int i = 0; i < resolvedFiles.size(); i++) {
                SmartFile sf = resolvedFiles.get(i);
                Assert.assertEquals(msg, expectedFiles[i], sf.getVpath());
            }
        }
    }


    private void removeAndAddSomeFiles()
            throws IOException {
        File rootDir = tempFolder.getRoot();
        File file2 = new File(rootDir, "file2.txt");
        file2.delete();

        FileTestHelper.generateTestFile(new File(rootDir, "folder1/file9.txt"), 15);

        File folder2 = new File(rootDir, "folder1/ignorefolder");
        FileUtils.deleteDirectory(folder2);
    }

  /*  @Test // TODO: Find out if needed
    public void testScanDiff()
            throws Exception {
        createTestData();

        SmartScanner ds = new SmartScanner(tempFolder.getRoot(), null, null, 5);


        SimpleListReceiver first = new SimpleListReceiver();
        // we take the initial snapshot
        ds.scan(first);
        List<SmartFile> oldFiles = first.getResult();

        // now we change 3 files. add one and remove
        removeAndAddSomeFiles();

        SimpleListReceiver next = new SimpleListReceiver();
        ds.scan(next);

        DirectoryScanResult dsr = dss.diffIncludedFiles(oldFiles);

        String[] addedFiles = dsr.getFilesAdded();
        String[] removedFiles = dsr.getFilesRemoved();
        Assert.assertNotNull(addedFiles);
        Assert.assertNotNull(removedFiles);
        Assert.assertEquals(1, addedFiles.length);
        Assert.assertEquals(2, removedFiles.length);
    }
    */

    private static class SimpleListReceiver implements SmartFileReceiver {
        private final List<SmartFile> result = Collections.synchronizedList(new ArrayList<SmartFile>());


        public List<SmartFile> getResult() {
            return result;
        }

        @Override
        public void accept(SmartFile file) {
            result.add(file);
        }
    }
}
