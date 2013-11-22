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
 */package org.smartscan.api;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;

public class Java7SmartFile implements SmartFile {
    private static BasicFileAttributes basicFileAttributes;
    private final File file;

    private final char[][] fileName;

    private Java7SmartFile(File file, char[][] parentVpath)
    {
        this.file = file;
		//noinspection AssignmentToCollectionOrArrayFieldFromParameter
		fileName = parentVpath;
    }

	public static SmartFile createSmartFile(File file, char[][] parentVpath, BasicFileAttributes basicFileAttributes) {
        Java7SmartFile.basicFileAttributes = basicFileAttributes;
        return new Java7SmartFile(file, parentVpath);
	}


    @Override
    public boolean isFile() {
        return basicFileAttributes.isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return basicFileAttributes.isDirectory();
    }

    public File getFile()
    {
        return file;
    }

    public String getVpath(){
        StringBuilder result = new StringBuilder();
        for (char[] chars : fileName) {
            result.append(chars);
            result.append(File.separatorChar);
        }
        result.append( file.getName());
        return result.toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO: Cant do this for proxies
        return file.getPath().compareTo( ((Java7SmartFile)o).file.getPath());
    }

    @Override
    public String toString() {
        return getVpath();
    }
}
