package org.smartscan.tools;


import java.io.File;

public class ModernBase
{
    protected final File basedir;

    protected final Filters includesPatterns;

    protected final Filters excludesPatterns;

    protected ModernBase( File basedir, Filters includes, Filters excludes )
	{
		this.basedir = basedir;
		includesPatterns =  includes ;
		excludesPatterns = excludes;
	}

	protected boolean couldHoldIncluded( char[][] tokenizedvpath )
    {
        return includesPatterns.matchesPatternStart( tokenizedvpath);
    }

    protected static void doSleep( int millis )
    {
        try
        {
            //noinspection ImplicitNumericConversion
            Thread.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    public boolean shouldInclude( char[][] tokenizedVpath ){
        return includesPatterns.matches( tokenizedVpath)
            && !excludesPatterns.matches( tokenizedVpath);
    }

    public static char[][] copy( char[][] original )
    {
        int length = original.length;
        char[][] result = new char[length][];
        System.arraycopy( original, 0, result, 0, length );
        return result;
    }

    public static char[][] copyWithOneExtra( char[][] original )
    {
        int length = original.length;
        char[][] result = new char[length + 1][];
        System.arraycopy( original, 0, result, 0, length );
        return result;
    }



}
