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
package org.rosenvold.exp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * Base class for testcases doing tests with files.
 *
 * @author Jeremias Maerki
 */
public abstract class FileBasedTestCase
    extends TestCase
{
    private static File testDir;

    public static File getTestDirectory()
    {
        if ( testDir == null )
        {
            testDir = ( new File( "target/test/io/" ) ).getAbsoluteFile();
        }
        return testDir;
    }

    protected byte[] createFile( File file, long size )
        throws IOException
    {
        if ( !file.getParentFile().exists() )
        {
            throw new IOException( "Cannot create file " + file + " as the parent directory does not exist" );
        }

        byte[] data = generateTestData( size );

        BufferedOutputStream output = new BufferedOutputStream( new FileOutputStream( file ) );

        try
        {
            output.write( data );

            return data;
        }
        finally
        {
            output.close();
        }
    }

    protected byte[] generateTestData( long size )
    {
        try
        {
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            generateTestData( baout, size );
            return baout.toByteArray();
        }
        catch ( IOException ioe )
        {
            throw new RuntimeException( "This should never happen: " + ioe.getMessage() );
        }
    }

    protected void generateTestData( OutputStream out, long size )
        throws IOException
    {
        for ( int i = 0; i < size; i++ )
        {
            //output.write((byte)'X');

            // nice varied byte pattern compatible with Readers and Writers
            out.write( (byte) ( ( i % 127 ) + 1 ) );
        }
    }

    // ----------------------------------------------------------------------
    // Assertions
    // ----------------------------------------------------------------------

}
