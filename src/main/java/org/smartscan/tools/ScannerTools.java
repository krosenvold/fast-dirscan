package org.smartscan.tools;

import org.codehaus.plexus.util.StringUtils;

import java.io.File;

public final class ScannerTools
{

    public static final String[] NONE = new String[0];

    private ScannerTools()
    {
    }

    /**
     * Normalizes the pattern, e.g. converts forward and backward slashes to the platform-specific file separator.
     *
     * @param pattern The pattern to normalize, must not be {@code null}.
     * @return The normalized pattern, never {@code null}.
     */
    @SuppressWarnings( { "HardcodedFileSeparator", "AssignmentToMethodParameter" } )
    public static String normalizePattern( String pattern )
    {
        pattern = pattern.trim();

        if ( pattern.startsWith( SelectorUtils.REGEX_HANDLER_PREFIX ) )
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

    public static String[] getIncludes( String... includes )
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

    public static String[] getExcludes( String... excludes )
    {
        if ( excludes == null )
        {
            return NONE;
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

    @SuppressWarnings( "DuplicateStringLiteralInspection" )
    public static void verifyBaseDir( File basedir )
    {
        if ( basedir == null )
        {
            throw new IllegalStateException( "No basedir set" );
        }
        if ( !basedir.exists() )
        {
            throw new IllegalStateException( "basedir " + basedir + " does not exist" );
        }
        if ( !basedir.isDirectory() )
        {
            throw new IllegalStateException( "basedir " + basedir + " is not a directory" );
        }
    }
}
