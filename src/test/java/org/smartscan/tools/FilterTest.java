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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.smartscan.tools.Filter.tokenizePathToCharArray;

/**
 * @author Kristian Rosenvold
 */
public class FilterTest
{
    @Test
    public void matchPath()
        throws Exception
    {
        Filter mp = new Filter( "ABC*" );
        assertTrue( mp.matchAntPath(tokenizePathToCharArray("ABCD", 'x'), true) );
    }

    @Test
    public void testTokenizeEmptyStringCharArray()
            throws Exception
    {
        char[][] chars = tokenizePathToCharArray("", 'x', 0);
        assertEquals( 0, chars.length);
    }


    @Test
    public void testTokenizeCharArray()
            throws Exception
    {
        char[][] chars = tokenizePathToCharArray("ABCxDEFxEEEx", 'x');
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EEE", new String(chars[2]));
    }

    @Test
    public void testTokenizeCharArrayNoEndToken()
            throws Exception
    {
        char[][] chars = tokenizePathToCharArray("ABCxDEFxEER", 'x');
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EER", new String(chars[2]));
    }

    @Test
    public void testTokenizeltiDelim()
            throws Exception
    {
        char[][] chars = tokenizePathToCharArray("xxABCxDEFxEER", 'x');
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EER", new String(chars[2]));
    }

}
