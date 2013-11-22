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
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Describes a match target for SelectorUtils.
 * <p/>
 * Significantly more efficient than using strings, since re-evaluation and re-tokenizing is avoided.
 *
 * @author Kristian Rosenvold
 */
@ThreadSafe
public class Filter
{
    @Nonnull
    private final String source;

    @Nullable
    private final Pattern regexPattern;

    @Nonnull
    private final char[][] tokenizedChar;

	public Filter(@Nonnull String source)
    {
        //noinspection AssignmentToNull
        String regexPatternStr = SelectorUtils.isRegexPrefixedPattern( source ) ? source.substring(
            SelectorUtils.REGEX_HANDLER_PREFIX.length(),
            source.length() - SelectorUtils.PATTERN_HANDLER_SUFFIX.length() ) : null;
        regexPattern = regexPatternStr != null ? Pattern.compile( regexPatternStr ) : null;
        this.source =
            SelectorUtils.isAntPrefixedPattern( source )
                ? source.substring( SelectorUtils.ANT_HANDLER_PREFIX.length(), source.length()
                - SelectorUtils.PATTERN_HANDLER_SUFFIX.length() )
                : source;
        //noinspection HardcodedFileSeparator
        String altStr = source.replace( '\\', '/' );
        tokenizedChar = tokenizePathToCharArray( this.source, File.separatorChar );
    }


    private static boolean separatorPatternStartSlashMismatch( Filter filter, char[][] vpath, char separator )
    {
        boolean vpathStartsWithSeparator = vpath[0][0] == separator;
        boolean matchPatternStartsWithSeparator = filter.startsWith( separator );
        return vpath.length > 0 && vpathStartsWithSeparator != matchPatternStartsWithSeparator;
    }


    @SuppressWarnings( { "IfMayBeConditional", "TypeMayBeWeakened" } )
    boolean matchRegexPath( @Nonnull String path )
    {
            return regexPattern != null &&  regexPattern.matcher( path ).matches();
    }

    @SuppressWarnings( "IfMayBeConditional" )
    boolean matchAntPath( char[][] tokenizedVpath, boolean isCaseSensitive )
    {
        return SelectorUtils.matchAntPathPattern( tokenizedChar, tokenizedVpath, isCaseSensitive );
    }

    @SuppressWarnings( "HardcodedFileSeparator" )
    public boolean matchPatternStart( char[][] tokenizedvpath, boolean isCaseSensitive )
    {
        if ( regexPattern != null )
        {
            // Cant do this yet. Need file
            return true;
        }
        else
        {
            return matchAntPathPatternStart( this, tokenizedvpath, File.separatorChar, isCaseSensitive )
                || matchAntPathPatternStart( this, tokenizedvpath, '/', isCaseSensitive );
        }
    }

    private static boolean matchAntPathPatternStart( Filter pattern, char[][] vpath, char separator,
                                             boolean isCaseSensitive )
    {

        if ( separatorPatternStartSlashMismatch( pattern, vpath, separator ) )
        {
            return false;
        }

        return SelectorUtils.matchAntPathPatternStart( pattern.tokenizedChar, vpath, isCaseSensitive );
    }

    private boolean startsWith( char thechar )
    {
        return !source.isEmpty() && source.charAt( 0 ) == thechar;
    }

    static char[][] tokenizePathToCharArray( String source, char separator )
    {
        return tokenizePathToCharArray( source, separator, 0);
    }


    boolean usesRegex()
    {
        return regexPattern != null;
    }

    public static char[][] tokenizePathToCharArray( String path, char separator, int additional )
    {
        char[] pathCHar = path.toCharArray();
        int pathLen = pathCHar.length;
        int cnt = 0;
        for ( char aPathCHar : pathCHar )
        {
            if ( aPathCHar == separator )
            {
                cnt++;
            }
        }
        cnt++;
        List<char[]> result = new ArrayList<char[]>(cnt);

        for ( int i = 0; i < pathLen; i++ )
        {
            int j = i;
            int len = 0;
            while ( j < pathLen && pathCHar[ j ] != separator )
            {
                len++;
                j++;
            }
            if ( len > 0 )
            {
                char[] outp = new char[len];
                System.arraycopy( pathCHar, i, outp, 0, len );
                result.add( outp );
                i += len;
            }
        }
        return result.toArray( new char[result.size() +additional][] );
    }


}
