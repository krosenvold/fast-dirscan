package org.smartscan;

import org.smartscan.reference.MatchPatterns;
import org.smartscan.reference.ScannerTools;

import java.io.File;

public class ModernBase
{
    protected final File basedir;

    protected final MatchPatterns includesPatterns;

    protected final MatchPatterns excludesPatterns;

    public static final String POISON = "*POISON*";

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    private boolean isCaseSensitive = true;

    public ModernBase( File basedir, String[] includes, String[] excludes )
    {
        this.basedir = basedir;
        includesPatterns = MatchPatterns.from( ScannerTools.getIncludes( includes ) );
        excludesPatterns = MatchPatterns.from( ScannerTools.getExcludes( excludes ) );
    }

    protected boolean isIncluded( String name )
    {
        return includesPatterns.matches( name, isCaseSensitive );
    }

    protected boolean couldHoldIncluded( String name, String[] tokenized )
    {
        return includesPatterns.matchesPatternStart(name, tokenized, isCaseSensitive);
    }
    protected boolean couldHoldIncluded( String name, char[][] tokenized )
    {
        return includesPatterns.matchesPatternStart(name, tokenized, isCaseSensitive);
    }

    protected boolean isExcluded( String name )
    {
        return excludesPatterns.matches( name, isCaseSensitive );
    }

    protected void doSleep( int millis )
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }


    public boolean shouldInclude( String name, char[][] tokenized ){

        return includesPatterns.matches( name, tokenized, isCaseSensitive )
            && !excludesPatterns.matches( name, tokenized, isCaseSensitive );
    }

}
