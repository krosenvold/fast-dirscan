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
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of patterns to be matched
 *
 * @author Kristian Rosenvold
 */
@ThreadSafe
public class Filters
{

    private final Filter[] antPatterns;

    private final Filter[] regexPatterns;

    public Filters(Filter... antPatterns)
    {
        List<Filter> ant = new ArrayList<>( antPatterns.length );
        List<Filter> regex = new ArrayList<>( antPatterns.length );
        for ( Filter pattern : antPatterns )
        {
            if ( pattern.usesRegex() )
            {
                regex.add( pattern );
            }
            else
            {
                ant.add( pattern );
            }
        }

        this.antPatterns = ant.toArray( new Filter[ant.size()] );
        regexPatterns = regex.toArray( new Filter[regex.size()] );
    }


    public boolean matches( char[][] tokenizedVpath, boolean isCaseSensitive )
    {
        for ( Filter pattern : antPatterns )
        {
            if ( pattern.matchAntPath( tokenizedVpath, isCaseSensitive ) )
            {
                return true;
            }
        }
        if ( regexPatterns.length > 0 )
        {
            StringBuilder vpathB = new StringBuilder( sizeOf( tokenizedVpath, 1 ) );
            for ( char[] chars : tokenizedVpath )
            {
                vpathB.append( chars );
                vpathB.append( File.separatorChar );
            }
            String vpath2 = vpathB.toString();
            for ( Filter pattern : regexPatterns )
            {
                if ( pattern.matchRegexPath( vpath2 ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static int sizeOf( char[][] array, int padding )
    {
        int result = 0;
        for ( char[] chars : array )
        {
            result += chars.length;
            result += padding;
        }
        return result;
    }

    public boolean matchesPatternStart( char[][] tokenizedVpath, boolean isCaseSensitive )
    {
        if ( regexPatterns.length > 0 )
        {
            return true;
        }
        for ( Filter includesPattern : antPatterns )
        {
            if ( includesPattern.matchPatternStart( tokenizedVpath, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    public static @Nonnull
	Filters from( String... sources )
    {
        final int length = sources.length;
        Filter[] result = new Filter[length];
        for ( int i = 0; i < length; i++ )
        {
            result[i] = new Filter(sources[i]);
        }
        return new Filters( result );
    }

}
