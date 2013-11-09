package org.rosenvold.reference;

import org.codehaus.plexus.util.*;

import java.io.File;

/**
 * Created by kristian on 09.11.13.
 */
public class ScannerTools
{
    /**
     * Normalizes the pattern, e.g. converts forward and backward slashes to the platform-specific file separator.
     *
     * @param pattern The pattern to normalize, must not be <code>null</code>.
     * @return The normalized pattern, never <code>null</code>.
     */
    public static String normalizePattern( String pattern )
    {
        pattern = pattern.trim();

        if ( pattern.startsWith( org.codehaus.plexus.util.SelectorUtils.REGEX_HANDLER_PREFIX ) )
        {
            if ( File.separatorChar == '\\' )
            {
                pattern = StringUtils.replace( pattern, "/", "\\\\" );
            }
            else
            {
                pattern = StringUtils.replace( pattern, "\\\\", "/" );
            }
        }
        else
        {
            pattern = pattern.replace( File.separatorChar == '/' ? '\\' : '/', File.separatorChar );

            if ( pattern.endsWith( File.separator ) )
            {
                pattern += "**";
            }
        }

        return pattern;
    }

    public static String[] getIncludes( String[] includes )
    {
        final String[] inc;
        if ( includes == null )
        {
            inc = new String[1];
            inc[0] = "**";
            return inc;
        }
        else
        {
            inc = new String[includes.length];
            for ( int i = 0; i < includes.length; i++ )
            {
                inc[i] = normalizePattern( includes[i] );
            }
            return inc;
        }
    }

    public static String[] getExcludes( String[] excludes )
    {
        if ( excludes == null )
        {
            return new String[0];
        }
        else
        {
            String[] exc = new String[excludes.length];
            for ( int i = 0; i < excludes.length; i++ )
            {
                exc[i] = normalizePattern( excludes[i] );
            }
            return exc;
        }
    }
}
