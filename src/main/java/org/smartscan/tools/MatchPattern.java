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


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * Describes a match target for SelectorUtils.
 * <p/>
 * Significantly more efficient than using strings, since re-evaluation and re-tokenizing is avoided.
 *
 * @author Kristian Rosenvold
 */
public class MatchPattern
{
    @Nonnull
    private final String source;

    @Nonnull
    private final String altStr;

    @Nullable
    private final String regexPattern;

    @Nonnull
    private final char[][] tokenizedChar;

    private MatchPattern( @Nonnull String source, @Nonnull String separator )
    {
        //noinspection AssignmentToNull
        regexPattern = SelectorUtils.isRegexPrefixedPattern( source ) ? source.substring(
            SelectorUtils.REGEX_HANDLER_PREFIX.length(),
            source.length() - SelectorUtils.PATTERN_HANDLER_SUFFIX.length() ) : null;
        this.source =
            SelectorUtils.isAntPrefixedPattern( source )
                ? source.substring( SelectorUtils.ANT_HANDLER_PREFIX.length(), source.length()
                - SelectorUtils.PATTERN_HANDLER_SUFFIX.length() )
                : source;
        //noinspection HardcodedFileSeparator
        altStr = source.replace( '\\', '/' );
        tokenizedChar =  tokenizePathToCharArray( this.source, separator );
    }


    @SuppressWarnings( "IfMayBeConditional" )
    boolean matchPath( String str, char[][] strDirs, boolean isCaseSensitive )
    {
        if ( regexPattern != null )
        {
            return str.matches( regexPattern );
        }
        else
        {
            return SelectorUtils.matchAntPathPattern( tokenizedChar, strDirs, isCaseSensitive );
        }
    }

    @SuppressWarnings( "HardcodedFileSeparator" )
    public boolean matchPatternStart( String str, char[][] strDirs, boolean isCaseSensitive )
    {
        if ( regexPattern != null )
        {
            // Cant do this yet. Need file
            return true;
        }
        else
        {
            return SelectorUtils.matchAntPathPatternStart( this, str, strDirs, File.separator, isCaseSensitive )
                || SelectorUtils.matchAntPathPatternStart( this, altStr, strDirs, "/", isCaseSensitive );
        }
    }

    public char[][] getTokenizedPathStringChar()
    {
        return tokenizedChar;
    }

    public boolean startsWith( String string )
    {
        return source.startsWith( string );
    }


    public static char[][] tokenizePathToCharArray( String source, String separator )
    {
        String[] tokenized = SelectorUtils.tokenizePathToString( source, separator );
        char[][] tokenizedChar = new char[tokenized.length][];
        for ( int i = 0; i < tokenized.length; i++ )
        {
            tokenizedChar[i] = tokenized[i].toCharArray();
        }
        return tokenizedChar;
    }


    public static MatchPattern fromString( String source )
    {
        return new MatchPattern( source, File.separator );
    }

}
