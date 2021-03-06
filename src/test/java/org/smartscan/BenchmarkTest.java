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
package org.smartscan;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Assert;
import org.junit.Test;
import org.smartscan.api.SmartFile;
import org.smartscan.api.SmartFileReceiver;

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
		final File file = new File( "src/test/testdata/perftestData" );
        final int expected = scanOriginal( file, false).length;
        System.out.println( "Warmup complete" );
        for ( int i = 0; i < 10; i++ )
        {
            //assertThat( scanOriginal( file, false).length ).as( "original result" ).isEqualTo(expected);
            //assertThat( scanOriginal( file, true).length ).as( "w excl" ).isEqualTo(expected);
            Assert.assertEquals("12 mtsr", expected + 1, multiThreadedSingleReceiver(file, 12));
            assertThat( multiThreadedSingleReceiver(file, 4) ).as( "4 mtsr" ).isEqualTo( expected +1 );
            assertThat( cachingMultiThreaded(file, 10, false) ).as( "cmr" ).isEqualTo( expected);
			assertThat( cachingMultiThreaded(file, 12, false) ).as( "cmr" ).isEqualTo( expected);
            assertThat( cachingMultiThreaded(file, 16, false) ).as( "cmr" ).isEqualTo( expected);
            assertThat( cachingMultiThreaded(file, 12, true) ).as( "cmrwex " );
            assertThat( benchmarkMultiThreaded(file, 12) ).as( "cmr" ).isEqualTo( expected);
            System.out.println( "" );
        }

    }



    private static int multiThreadedSingleReceiver( File basedir, int nThreads )
        throws InterruptedException
    {
        long milliStart = System.currentTimeMillis();
        ConcurrentFileReceiver ffr = new ConcurrentFileReceiver();
        try
        {
			SmartScanner ss = new SmartScanner(basedir, null, null, nThreads);
			 final AtomicInteger recvd = new AtomicInteger( 0 );

			for (SmartFile s : ss.scan()) {
				recvd.incrementAndGet();
			}
			return recvd.incrementAndGet();
        }
        finally
        {
            System.out.print( ", MRSW" + nThreads + "(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }

    private static int cachingMultiThreaded(File basedir, int nThreads, boolean addDfeaultExcludes)
			throws InterruptedException
	{
		long milliStart = System.currentTimeMillis();
		ConcurrentFileReceiver ffr = new ConcurrentFileReceiver();
		try
		{
			SmartScanner ss = new SmartScanner(basedir, null, null, nThreads);
            if (addDfeaultExcludes) ss.addDefaultExcludes();

			ss.scan(ffr);

			return ffr.recvd.get();
		}
		finally
		{
			System.out.print( ", CMR" + nThreads + "(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
		}
	}

    private static int benchmarkMultiThreaded( File basedir, int nThreads )
            throws InterruptedException
    {
        long milliStart = System.currentTimeMillis();
        ConcurrentFileReceiver ffr = new ConcurrentFileReceiver();
        try
        {
            SmartScanner ss = new SmartScanner(basedir, null, null, nThreads);

            ss.scanReference(ffr);

            return ffr.recvd.get();
        }
        finally
        {
            System.out.print( ", RMR" + nThreads + "(" + ffr.firstSeenAt + ")=" + ( System.currentTimeMillis() - milliStart ) );
        }
    }


    static class ConcurrentFileReceiver
        implements SmartFileReceiver
    {
        private SmartFile first;

        private long firstSeenAt;

        long milliStart = System.currentTimeMillis();

        private final AtomicInteger recvd = new AtomicInteger( 0 );

        public void accept( SmartFile file )
        {
            if ( first == null )
            {
                firstSeenAt = System.currentTimeMillis() - milliStart;
                first = file;
            }
            recvd.incrementAndGet();
        }
    }

	private static String[] scanOriginal(File file, boolean addDefaultExcludes)
    {
        long start = System.currentTimeMillis();
        try
        {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setIncludes( null );
            directoryScanner.setExcludes( null );
            directoryScanner.setBasedir( file );
            directoryScanner.scan();
            if (addDefaultExcludes) directoryScanner.addDefaultExcludes();

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
