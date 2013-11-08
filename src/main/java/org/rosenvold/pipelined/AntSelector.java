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

package org.rosenvold.pipelined;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author Kristian Rosenvold
 */
public class AntSelector
    implements Selector
{

    private final boolean isCaseSensitive;

    private final String pattern;

    private final Vector patDirs;

    public AntSelector( String pattern, boolean caseSensitive )
    {
        this.isCaseSensitive = caseSensitive;
        this.pattern = pattern;
        patDirs = tokenizePath( pattern, File.separator );
    }

    public boolean matches( String string )
    {
        return matchAntPathPattern( pattern, string, isCaseSensitive );
    }

    public boolean matchPatternStart( String str )
    {
        String altStr = str.replace( '\\', '/' );

        return matchAntPathPatternStart( pattern, str, File.separator, isCaseSensitive ) || matchAntPathPatternStart(
            pattern, altStr, "/", isCaseSensitive );
    }

    private boolean matchAntPathPattern( String pattern, String str, boolean isCaseSensitive )
    {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if ( str.startsWith( File.separator ) != pattern.startsWith( File.separator ) )
        {
            return false;
        }

        Vector strDirs = tokenizePath( str, File.separator );

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String) patDirs.elementAt( patIdxStart );
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, (String) strDirs.elementAt( strIdxStart ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs.elementAt( i ).equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            if ( patIdxStart > patIdxEnd )
            {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String) patDirs.elementAt( patIdxEnd );
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, (String) strDirs.elementAt( strIdxEnd ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs.elementAt( i ).equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }

        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patDirs.elementAt( i ).equals( "**" ) )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
            for ( int i = 0; i <= strLength - patLength; i++ )
            {
                for ( int j = 0; j < patLength; j++ )
                {
                    String subPat = (String) patDirs.elementAt( patIdxStart + j + 1 );
                    String subStr = (String) strDirs.elementAt( strIdxStart + i + j );
                    if ( !match( subPat, subStr, isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if ( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( !patDirs.elementAt( i ).equals( "**" ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match( String pattern, String str )
    {
        return match( pattern, str, true );
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern         The pattern to match against.
     *                        Must not be <code>null</code>.
     * @param str             The string which must be matched against the pattern.
     *                        Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match( String pattern, String str, boolean isCaseSensitive )
    {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for ( int i = 0; i < patArr.length; i++ )
        {
            if ( patArr[i] == '*' )
            {
                containsStar = true;
                break;
            }
        }

        if ( !containsStar )
        {
            // No '*'s, so we make a shortcut
            if ( patIdxEnd != strIdxEnd )
            {
                return false; // Pattern and string do not have the same size
            }
            for ( int i = 0; i <= patIdxEnd; i++ )
            {
                ch = patArr[i];
                if ( ch != '?' && !equals( ch, strArr[i], isCaseSensitive ) )
                {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if ( patIdxEnd == 0 )
        {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ( ( ch = patArr[patIdxStart] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxStart], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ( ( ch = patArr[patIdxEnd] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxEnd], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] == '*' )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
            for ( int i = 0; i <= strLength - patLength; i++ )
            {
                for ( int j = 0; j < patLength; j++ )
                {
                    ch = patArr[patIdxStart + j + 1];
                    if ( ch != '?' && !equals( ch, strArr[strIdxStart + i + j], isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if ( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( patArr[i] != '*' )
            {
                return false;
            }
        }
        return true;
    }

    /*
     * Tests whether two characters are equal.
     */
    private static boolean equals( char c1, char c2, boolean isCaseSensitive )
    {
        if ( c1 == c2 )
        {
            return true;
        }
        if ( !isCaseSensitive )
        {
            // NOTE: Try both upper case and lower case as done by String.equalsIgnoreCase()
            if ( Character.toUpperCase( c1 ) == Character.toUpperCase( c2 )
                || Character.toLowerCase( c1 ) == Character.toLowerCase( c2 ) )
            {
                return true;
            }
        }
        return false;
    }

    public static Vector tokenizePath( String path, String separator )
    {
        Vector ret = new Vector();
        StringTokenizer st = new StringTokenizer( path, separator );
        while ( st.hasMoreTokens() )
        {
            ret.addElement( st.nextToken() );
        }
        return ret;
    }


    private static boolean matchAntPathPatternStart( String pattern, String str, String separator,
                                                     boolean isCaseSensitive )
    {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if ( str.startsWith( separator ) != pattern.startsWith( separator ) )
        {
            return false;
        }

        Vector patDirs = tokenizePath( pattern, separator );
        Vector strDirs = tokenizePath( str, separator );

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String) patDirs.elementAt( patIdxStart );
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, (String) strDirs.elementAt( strIdxStart ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            return true;
        }
        else //noinspection RedundantIfStatement
            if ( patIdxStart > patIdxEnd )
        {
            // String not exhausted, but pattern is. Failure.
            return false;
        }
        else
        {
            // pattern now holds ** while string is not exhausted
            // this will generate false positives but we can live with that.
            return true;
        }
    }

}
