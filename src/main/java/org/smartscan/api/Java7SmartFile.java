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

public class Java7SmartFile extends BaseJava7SmartFile implements SmartFile {
    private final BasicFileAttributes basicFileAttributes;

	private Java7SmartFile(File basedir, char[][] parentVpath)
    {
		this(basedir, parentVpath, basedir.getName().toCharArray());
    }

	protected Java7SmartFile(File basedir, char[][] parentVpath, char[] fileName) {
		super(basedir, parentVpath, fileName);
		basicFileAttributes = getBasicFileAttributes(file);
	}



	public static SmartFile createSmartFile(File file, char[][] parentVpath) {
        return new Java7SmartFile(file, parentVpath);
	}

	public static final char[][] NO_FILES_VPATH_ = new char[0][];

	public static SmartFile createRootDir(File file) {
		return new Java7SmartFile(file, NO_FILES_VPATH_, new char[0]);
	}

	@Override
    public boolean isFile() {
        return basicFileAttributes.isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return basicFileAttributes.isDirectory();
    }

	@Override
	public long lastModified(){
		return basicFileAttributes.lastModifiedTime().toMillis();
	}
}
