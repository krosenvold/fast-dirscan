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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.rosenvold.multithreadedreaxer.MultiThreadedScannerReader;
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
        assertThat(1).isEqualTo( 1 );
        final File file = new File( System.getProperty( "user.home" ), "fastdirscan-testdata" );
        final int expected = scanOriginal( file ).length;
        System.out.println( "Warmup complete" );
        for ( int i = 0; i < 5; i++ )
        {
            assertThat( scanOriginal( file ).length ).as( "original result" ).isEqualTo( expected);
            assertThat( multiThreadedReader( file, 12 ) ).describedAs( "12 threads" ).isEqualTo( expected + 1);
            assertThat( multiThreadedReader( file, 8 ) ).as("8 threads").isEqualTo( expected +1 );
            System.out.println("");
        }

    }

    private static int multiThreadedReader( File basedir, int nThreads )
        throws InterruptedException
    {
        long first = 0;
        long start = System.nanoTime();
        long milliStart = System.currentTimeMillis();
        try
        {
            MultiThreadedScannerReader
                pipelinedDirectoryScanner = new MultiThreadedScannerReader( basedir, null, null, nThreads );
            pipelinedDirectoryScanner.scanThreaded();
            ConcurrentLinkedQueue queue = pipelinedDirectoryScanner.getQueue();

            String take;
            int i = 0;
            do
            {
                take = (String) queue.poll();
                if ( take != null )
                {
                    i++;
                    if ( i == 1 )
                    {
                        first = (System.nanoTime() - start) / 1000;
                    }
                }
            }
            while ( take != MultiThreadedScannerReader.POISON );
            return i;
        }
        finally
        {
            System.out.print( ", multiThreadedReader, " + nThreads + " threads (" + first + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }


    private String[] scanOriginal( File file )
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
