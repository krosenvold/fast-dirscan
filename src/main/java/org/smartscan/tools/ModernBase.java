package org.smartscan.tools;


import java.io.File;

public class ModernBase
{
    protected final File basedir;

    protected final Filters includesPatterns;

    protected final Filters excludesPatterns;

    public static final String POISON = "*POISON*";

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    private static final boolean isCaseSensitive = true;

    @SuppressWarnings( "MethodCanBeVariableArityMethod" )
    public ModernBase( File basedir, String[] includes, String[] excludes )
    {
        this.basedir = basedir;
        includesPatterns = Filters.from(ScannerTools.getIncludes(includes));
        excludesPatterns = Filters.from(ScannerTools.getExcludes(excludes));
    }

	protected ModernBase( File basedir, Filters includes, Filters excludes )
	{
		this.basedir = basedir;
		includesPatterns =  includes ;
		excludesPatterns = excludes;
	}

	protected boolean couldHoldIncluded( char[][] tokenizedvpath )
    {
        return includesPatterns.matchesPatternStart( tokenizedvpath, isCaseSensitive);
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
        return includesPatterns.matches( tokenizedVpath, isCaseSensitive )
            && !excludesPatterns.matches( tokenizedVpath, isCaseSensitive );
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
