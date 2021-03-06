package org.smartscan.tools;

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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.smartscan.tools.Filter.tokenizePathToCharArray;

/**
 * @author Kristian Rosenvold
 */
public class FiltersTest
{
    @Test
    public void matches()
        throws Exception
    {
        Filters from = Filters.from(true, "ABC**", "CDE**" );
        assertTrue( from.matches( tokenizePathToCharArray("ABCDE", 'x')) );
        assertTrue( from.matches( tokenizePathToCharArray("CDEF", 'x')) );
        assertFalse(from.matches(tokenizePathToCharArray("XYZ", 'x')));
    }

	@Test
	public void append(){
		Filters s1 = Filters.from(true, "ABC**", "CDE**" );
		Filters s2 = Filters.from(true, "XX**", "YY**" );
		Filters result = s1.append( s2);
		assertTrue( result.matches( tokenizePathToCharArray("ABCDE", 'x')) );
		assertTrue( result.matches( tokenizePathToCharArray("CDE", 'x')) );
		assertTrue( result.matches( tokenizePathToCharArray("XXA", 'x')) );
		assertTrue( result.matches( tokenizePathToCharArray("YYZ", 'x')) );

	}

	@Test
	public void appendEmpty(){
		Filters s1 = Filters.from(true, "ABC**");
		Filters result = s1.append(Filters.from(true));
		assertTrue( result.matches( tokenizePathToCharArray("ABCDE", 'x')) );


	}

}
