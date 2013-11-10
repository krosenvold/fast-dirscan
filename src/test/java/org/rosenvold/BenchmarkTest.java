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
package org.rosenvold;

import java.io.File;

import org.junit.Test;
import org.rosenvold.reference.DirectoryScanner;

import static org.fest.assertions.api.Assertions.*;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UseOfSystemOutOrSystemErr", "OverlyBroadThrowsClause" } )
public class BenchmarkTest
{
    @Test
    public void doRunBenchmarks()
        throws Exception
    {
        assertThat( 1 ).isEqualTo( 1 );
        final File file = new File( System.getProperty( "user.home" ), "fastdirscan-testdata" );
        final int expected = scanOriginal( file ).length;
        System.out.println( "Warmup complete" );
        for ( int i = 0; i < 10; i++ )
        {
            assertThat( scanOriginal( file ).length ).as( "original result" ).isEqualTo( expected );
            assertThat( singleReaderSingleWorker( file ) ).as( "srsw" ).isEqualTo( expected );
            assertThat( multiThreadedSingleReceiver( file, 12 ) ).describedAs( "12 mtsr" ).isEqualTo( expected  );
            assertThat( multiThreadedSingleReceiver( file, 8 ) ).as( "8 mtsr" ).isEqualTo( expected );
            assertThat( multiThreadedSingleReceiver( file, 4 ) ).as( "4 mtsr" ).isEqualTo( expected );
            assertThat( multiThreaded( file, 12 ) ).as( "mr" ).isEqualTo( expected );

            System.out.println( "" );
        }

    }

    private static int multiThreadedSingleReceiver( File basedir, int nThreads )
        throws InterruptedException
    {
        long milliStart = System.currentTimeMillis();
        MyFileReceiver ffr = new MyFileReceiver();
        try
        {
            MultiReaderSingleWorker pipelinedDirectoryScanner =
                new MultiReaderSingleWorker( basedir, null, null, nThreads );

            pipelinedDirectoryScanner.scanThreaded();
            pipelinedDirectoryScanner.getScanResult( ffr );
            pipelinedDirectoryScanner.close();
            return ffr.size;
        }
        finally
        {
            System.out.print( ", MRSW" + nThreads + "(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }

    private static int multiThreaded( File basedir, int nThreads )
        throws InterruptedException
    {
        long milliStart = System.currentTimeMillis();
        CocurrentFileReceiver ffr = new CocurrentFileReceiver();
        try
        {
            MultiReader scanner = new MultiReader( basedir, null, null, ffr, nThreads );

            scanner.scanThreaded();
            scanner.getScanResult(  );
            scanner.close();
            return ffr.size;
        }
        finally
        {
            System.out.print( ", MR" + nThreads + "(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }

    static class MyFileReceiver
        implements FastFileReceiver
    {
        private FastFile first;

        private long firstSeenAt;

        long milliStart = System.currentTimeMillis();

        volatile int size = 0;

        public void accept( FastFile file )
        {
            if ( first == null )
            {
                firstSeenAt = System.currentTimeMillis() - milliStart;
                first = file;
            }
            size++;
        }
    }

    static class CocurrentFileReceiver
        implements FastFileReceiver
    {
        private FastFile first;

        private long firstSeenAt;

        long milliStart = System.currentTimeMillis();

         int size = 0;

        public void accept( FastFile file )
        {
            if ( first == null )
            {
                firstSeenAt = System.currentTimeMillis() - milliStart;
                first = file;
            }
            synchronized ( this ){
            size++;
            }
        }
    }

    private static int singleReaderSingleWorker( File basedir )
        throws InterruptedException
    {
        long milliStart = System.currentTimeMillis();
        MyFileReceiver ffr = new MyFileReceiver();
        try
        {
            SingleReader fst = new SingleReader( basedir, null, null );
            fst.scanThreaded();
            fst.getScanResult( ffr );
            return ffr.size;
        }
        finally
        {
            System.out.print( ", SRSW(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }


    private static String[] scanOriginal( File file )
    {
        long start = System.currentTimeMillis();
        try
        {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setIncludes( null );
            directoryScanner.setExcludes( null );
            directoryScanner.setBasedir( file );
            directoryScanner.scan();

            final String[] includedFiles = directoryScanner.getIncludedFiles();
            int size = includedFiles.length;
            String foo;
            for ( int i = 0; i < size; i++ )
            {
                foo = includedFiles[i];
            }
            return includedFiles;
        }
        finally
        {
            final long elapsed = System.currentTimeMillis() - start;
            System.out.print( "Elapsed, old=" + elapsed );

        }
    }
}
