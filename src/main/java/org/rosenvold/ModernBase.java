package org.rosenvold;

import org.rosenvold.reference.MatchPatterns;
import org.rosenvold.reference.ScannerTools;

import java.io.File;

public class ModernBase
{
    protected final File basedir;

    protected final MatchPatterns includesPatterns;

    protected final MatchPatterns excludesPatterns;

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

    protected boolean couldHoldIncluded( String name )
    {
        return includesPatterns.matchesPatternStart(name, isCaseSensitive);
    }

    protected boolean isExcluded( String name )
    {
        return excludesPatterns.matches( name, isCaseSensitive );
    }

}
