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
package org.smartscan.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;

public class Java7SmartFile implements SmartFile {
    @Nonnull
    private final BasicFileAttributes fileAttributes;
    @Nullable
    private final BasicFileAttributes symlinkFileAttributes;
    private final File file;

    private final char[][] parentVpath;
    private final char[] fileNameChar;

    private Java7SmartFile(File file, char[][] parentVpath) {
        this.file = file;
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.parentVpath = parentVpath;
        BasicFileAttributes rawAttrs = readAttributes(file, LinkOption.NOFOLLOW_LINKS);
        if (rawAttrs.isSymbolicLink()) {
            symlinkFileAttributes = rawAttrs;
            fileAttributes = readAttributes(file);
        } else {
            fileAttributes = rawAttrs;
            symlinkFileAttributes = null;
        }
        fileNameChar = file.getName().toCharArray();
    }

    public static SmartFile createSmartFile(File file, char[][] parentVpath) {
        return new Java7SmartFile(file, parentVpath);
    }


    @Override
    public boolean isFile() {
        return fileAttributes.isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return fileAttributes.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return symlinkFileAttributes != null;
    }

    public File getFile() {
        return file;
    }

    public String getVpath() {
        StringBuilder result = new StringBuilder();
        for (char[] chars : parentVpath) {
            result.append(chars);
            result.append(File.separatorChar);
        }
        result.append(file.getName());
        return result.toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO: Cant do this for proxies
        return file.getPath().compareTo(((Java7SmartFile) o).file.getPath());
    }

    @Override
    public String toString() {
        return getVpath();
    }

    @Override
    public File[] listFiles() {
        return file.listFiles();
    }

    public char[] getFileNameChar() {
        return fileNameChar;
    }

    @Override
    public char[][] getParentVpath() {
        return parentVpath;
    }

    private static BasicFileAttributes readAttributes(File file, LinkOption... options) {
        try {
            return Files.readAttributes(file.toPath(), BasicFileAttributes.class, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
