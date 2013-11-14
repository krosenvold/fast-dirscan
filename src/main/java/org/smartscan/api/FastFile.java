package org.smartscan.api;

import java.io.File;

public class FastFile
{
    private final File file;

    private final char[][] fileName;

    public FastFile( File file, char[][] parentVpath )
    {
        this.file = file;
        fileName = parentVpath;
    }

    public File getFile()
    {
        return file;
    }
}
