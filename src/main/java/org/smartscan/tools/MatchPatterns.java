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

package org.smartscan.tools;

import java.io.File;


/**
 * A list of patterns to be matched
 *
 * @author Kristian Rosenvold
 */
public class MatchPatterns
{
    private static final char[] NOTHING = { };

    private final MatchPattern[] patterns;

    private MatchPatterns( MatchPattern... patterns )
    {
        this.patterns = patterns;
    }

    /**
     * Checks these MatchPatterns against a specified string.
     * <p/>
     * Uses far less string tokenization than any of the alternatives.
     *
     * @param name            The name to look for
     * @param isCaseSensitive If the comparison is case sensitive
     * @return true if any of the supplied patterns match
     */
    public boolean matches( String name, boolean isCaseSensitive )
    {
        String[] tokenized = SelectorUtils.tokenizePathToString( name, File.separator );
        return matches(  name, tokenized, isCaseSensitive );
    }

    public boolean matches( String name, String[] tokenizedName, boolean isCaseSensitive )
    {
        char[][] tokenizedNameChar = toChars( tokenizedName );
        for ( MatchPattern pattern : patterns )
        {
            if ( pattern.matchPath( name, tokenizedNameChar, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }


    public boolean matches( String name, char[][] tokenizedNameChar, boolean isCaseSensitive )
    {
        for ( MatchPattern pattern : patterns )
        {
            if ( pattern.matchPath( name, tokenizedNameChar, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    public static char[][] toChars( String... tokenizedName )
    {
        char[][] tokenizedNameChar = new char[tokenizedName.length][];
        for(int i = 0;  i < tokenizedName.length; i++){
            String s = tokenizedName[i];
            tokenizedNameChar[i] = s != null ? s.toCharArray() : NOTHING;
        }
        return tokenizedNameChar;
    }

    public boolean matchesPatternStart( String name, char[][] nameTokenized, boolean isCaseSensitive )
    {
        for ( MatchPattern includesPattern : patterns )
        {
            if ( includesPattern.matchPatternStart( name, nameTokenized, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    public static MatchPatterns from( String... sources )
    {
        final int length = sources.length;
        MatchPattern[] result = new MatchPattern[length];
        for ( int i = 0; i < length; i++ )
        {
            result[i] = MatchPattern.fromString( sources[i] );
        }
        return new MatchPatterns( result );
    }

}