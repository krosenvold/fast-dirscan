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

public final class SelectorUtils
{

    public static final String PATTERN_HANDLER_PREFIX = "[";

    public static final String PATTERN_HANDLER_SUFFIX = "]";

    public static final String REGEX_HANDLER_PREFIX = "%regex" + PATTERN_HANDLER_PREFIX;

    public static final String ANT_HANDLER_PREFIX = "%ant" + PATTERN_HANDLER_PREFIX;

    /**
     * Private Constructor
     */
    private SelectorUtils()
    {
    }


    static boolean matchAntPathPatternStart( char[][] patterns, char[][] vpath, boolean isCaseSensitive )
    {
        int patIdxStart = 0;
        int patIdxEnd = patterns.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = vpath.length - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            char[] patDir = patterns[patIdxStart];
            if ( isDoubleStar( patDir ) )
            {
                break;
            }
            if ( !match( patDir, vpath[strIdxStart], isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        return strIdxStart > strIdxEnd || patIdxStart <= patIdxEnd;
    }

    static boolean isRegexPrefixedPattern( String pattern )
    {
        return pattern.length() > REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1
            && pattern.startsWith( REGEX_HANDLER_PREFIX ) && pattern.endsWith( PATTERN_HANDLER_SUFFIX );
    }

    static boolean isAntPrefixedPattern( String pattern )
    {
        return pattern.length() > ANT_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1
            && pattern.startsWith( ANT_HANDLER_PREFIX ) && pattern.endsWith( PATTERN_HANDLER_SUFFIX );
    }

    static boolean matchAntPathPattern( char[][] patterns, char[][] directories, boolean isCaseSensitive )
    {
        int patIdxStart = 0;
        int patIdxEnd = patterns.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = directories.length - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            char[] pattern = patterns[patIdxStart];
            if ( isDoubleStar( pattern ) )
            {
                break;
            }
            if ( !match( pattern, directories[strIdxStart], isCaseSensitive ) )
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
                if ( !isDoubleStar( patterns[i] ) )
                {
                    return false;
                }
            }
            return true;
        }
        if ( patIdxStart > patIdxEnd )
        {
            // String not exhausted, but pattern is. Failure.
            return false;
        }

        // up to last '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            char[] pattern = patterns[patIdxEnd];
            if ( isDoubleStar( pattern ) )
            {
                break;
            }
            if ( !match( pattern, directories[strIdxEnd], isCaseSensitive ) )
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
                if ( !isDoubleStar( patterns[i] ) )
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
                if ( isDoubleStar( patterns[i] ) )
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
                    char[] subPat = patterns[patIdxStart + j + 1];
                    char[] subStr = directories[strIdxStart + i + j];
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
            if ( !isDoubleStar( patterns[i] ) )
            {
                return false;
            }
        }

        return true;
    }

    private static boolean isDoubleStar( char... patDir )
    {
        return patDir != null && patDir.length == 2 && patDir[0] == '*' && patDir[1] == '*';
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be {@code null}.
     * @param str     The string which must be matched against the pattern.
     *                Must not be {@code null}.
     * @return {@code true} if the string matches against the pattern,
     * or {@code false} otherwise.
     */
    public static boolean match( String pattern, String str )
    {
        return match( pattern, str, true );
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern         The pattern to match against.
     *                        Must not be {@code null}.
     * @param str             The string which must be matched against the pattern.
     *                        Must not be {@code null}.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return {@code true} if the string matches against the pattern,
     * or {@code false} otherwise.
     */
    public static boolean match( String pattern, String str, boolean isCaseSensitive )
    {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        return match( patArr, strArr, isCaseSensitive );
    }

    public static boolean match( char[] patArr, char[] strArr, boolean isCaseSensitive )
    {
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for ( char aPatArr : patArr )
        {
            if ( aPatArr == '*' )
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
            int patLength = patIdxTmp - patIdxStart - 1;
            int strLength = strIdxEnd - strIdxStart + 1;
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

    /**
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


}
