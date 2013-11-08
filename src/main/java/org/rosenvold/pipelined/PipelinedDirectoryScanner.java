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


import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for scanning a directory for files/directories which match certain
 * criteria.
 * <p/>
 * These criteria consist of selectors and patterns which have been specified.
 * With the selectors you can select which files you want to have included.
 * Files which are not selected are excluded. With patterns you can include
 * or exclude files based on their filename.
 * <p/>
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of selectors,
 * including special support for matching against filenames with include and
 * and exclude patterns. Only files/directories which match at least one
 * pattern of the include pattern list or other file selector, and don't match
 * any pattern of the exclude pattern list or fail to match against a required
 * selector will be placed in the list of files/directories found.
 * <p/>
 * When no list of include patterns is supplied, "**" will be used, which
 * means that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded. When
 * no selectors are supplied, none are applied.
 * <p/>
 * The filename pattern matching is done as follows:
 * The name to be matched is split up in path segments. A path segment is the
 * name of a directory or file, which is bounded by
 * <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * For example, "abc/def/ghi/xyz.java" is split up in the segments "abc",
 * "def","ghi" and "xyz.java".
 * The same is done for the pattern against which should be matched.
 * <p/>
 * The segments of the name and the pattern are then matched against each
 * other. When '**' is used for a path segment in the pattern, it matches
 * zero or more path segments of the name.
 * <p/>
 * There is a special case regarding the use of <code>File.separator</code>s
 * at the beginning of the pattern and the string to match:<br>
 * When a pattern starts with a <code>File.separator</code>, the string
 * to match must also start with a <code>File.separator</code>.
 * When a pattern does not start with a <code>File.separator</code>, the
 * string to match may not start with a <code>File.separator</code>.
 * When one of these rules is not obeyed, the string will not
 * match.
 * <p/>
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used:<br>
 * '*' matches zero or more characters<br>
 * '?' matches one character.
 * <p/>
 * Examples:
 * <p/>
 * "**\*.class" matches all .class files/dirs in a directory tree.
 * <p/>
 * "test\a??.java" matches all files/dirs which start with an 'a', then two
 * more characters and then ".java", in a directory called test.
 * <p/>
 * "**" matches everything in a directory tree.
 * <p/>
 * "**\test\**\XYZ*" matches all files/dirs which start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123").
 * <p/>
 * Case sensitivity may be turned off if necessary. By default, it is
 * turned on.
 * <p/>
 * Example of usage:
 * <pre>
 *   String[] includes = {"**\\*.class"};
 *   String[] excludes = {"modules\\*\\**"};
 *   ds.setIncludes(includes);
 *   ds.setExcludes(excludes);
 *   ds.setBasedir(new File("test"));
 *   ds.setCaseSensitive(true);
 *   ds.scan();
 *
 *   System.out.println("FILES:");
 *   String[] files = ds.getIncludedFiles();
 *   for (int i = 0; i < files.length; i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre>
 * This will scan a directory called test for .class files, but excludes all
 * files in all proper subdirectories of a directory called "modules"
 *
 * @author Arnout J. Kuiper
 *         <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 */
public class PipelinedDirectoryScanner
{

    /**
     * Patterns which should be excluded by default, like SCM files
     * <ul>
     * <li>Misc: &#42;&#42;/&#42;~, &#42;&#42;/#&#42;#, &#42;&#42;/.#&#42;, &#42;&#42;/%&#42;%, &#42;&#42;/._&#42; </li>
     * <li>CVS: &#42;&#42;/CVS, &#42;&#42;/CVS/&#42;&#42;, &#42;&#42;/.cvsignore</li>
     * <li>RCS: &#42;&#42;/RCS, &#42;&#42;/RCS/&#42;&#42;</li>
     * <li>SCCS: &#42;&#42;/SCCS, &#42;&#42;/SCCS/&#42;&#42;</li>
     * <li>VSSercer: &#42;&#42;/vssver.scc</li>
     * <li>SVN: &#42;&#42;/.svn, &#42;&#42;/.svn/&#42;&#42;</li>
     * <li>GNU: &#42;&#42;/.arch-ids, &#42;&#42;/.arch-ids/&#42;&#42;</li>
     * <li>Bazaar: &#42;&#42;/.bzr, &#42;&#42;/.bzr/&#42;&#42;</li>
     * <li>SurroundSCM: &#42;&#42;/.MySCMServerInfo</li>
     * <li>Mac: &#42;&#42;/.DS_Store</li>
     * <li>Serena Dimension: &#42;&#42;/.metadata, &#42;&#42;/.metadata/&#42;&#42;</li>
     * <li>Mercurial: &#42;&#42;/.hg, &#42;&#42;/.hg/&#42;&#42;</li>
     * <li>GIT: &#42;&#42;/.git, &#42;&#42;/.git/&#42;&#42;</li>
     * <li>Bitkeeper: &#42;&#42;/BitKeeper, &#42;&#42;/BitKeeper/&#42;&#42;, &#42;&#42;/ChangeSet, &#42;&#42;/ChangeSet/&#42;&#42;</li>
     * <li>Darcs: &#42;&#42;/_darcs, &#42;&#42;/_darcs/&#42;&#42;, &#42;&#42;/.darcsrepo, &#42;&#42;/.darcsrepo/&#42;&#42;&#42;&#42;/-darcs-backup&#42;, &#42;&#42;/.darcs-temp-mail
     * </ul>
     *
     * @see #addDefaultExcludes()
     */
    /**
     * The base directory to be scanned.
     */
    private final File basedir;


    /**
     * The patterns for the files to be included.
     */
    private final String[] includes;

    private final Selector[] includesSelector;

    /**
     * The patterns for the files to be excluded.
     */
    private final String[] excludes;

    private final Selector[] excludesSelector;

    private final PipelineApi pipelineApi;

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    private boolean isCaseSensitive = true;

    private final SelectorFactory selectorFactory = new SelectorFactory();

    private final ExecutorService executor;

    public static final String POISON = "*POISON*";


    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc
     */
    public PipelinedDirectoryScanner( File basedir, String[] includes, String[] excludes, PipelineApi pipelineApi )
    {
        this.basedir = basedir;
        this.includes = getIncludes( includes );
        this.includesSelector = null; //createSelectors( this.includes, isCaseSensitive );
        this.excludes = getExcludes( excludes );
        this.excludesSelector = null; //createSelectors( this.excludes, isCaseSensitive );
        this.pipelineApi = pipelineApi;
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

        this.executor = Executors.newFixedThreadPool( 12 );

    }

    private Selector[] createSelectors( String[] selectorPatterns, boolean caseSensitive )
    {
        final int size = selectorPatterns.length;
        Selector[] result = new Selector[size];
        for ( int i = 0; i < size; i++ )
        {
            result[i] = selectorFactory.create( selectorPatterns[i], caseSensitive );
        }
        return result;
    }

    /**
     * Tests whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p/>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern         The pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             The path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return whether or not a given path matches the start of a given
     *         pattern up to the first "**".
     */
    private static boolean matchPatternStart( String pattern, String str, boolean isCaseSensitive )
    {
        return org.rosenvold.exp.SelectorUtils.matchPatternStart( pattern, str, isCaseSensitive );
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern         The pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             The path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    private static boolean matchPath( String pattern, String str, boolean isCaseSensitive )
    {
        return org.rosenvold.exp.SelectorUtils.matchPath( pattern, str, isCaseSensitive );
    }

    /**
     * Scans the base directory for files which match at least one include
     * pattern and don't match any exclude patterns. If there are selectors
     * then the files must pass muster there, as well.
     *
     * @throws IllegalStateException if the base directory was set
     *                               incorrectly (i.e. if it is <code>null</code>, doesn't exist,
     *                               or isn't a directory).
     */
    public void scan()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            public void run()
            {
                asynchscandir( basedir, "" );
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
        thread.join();
        while (threadsStarted.get() > 0){
            Thread.sleep(10);
        }
    }

    public void scanThreaded()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            public void run()
            {
                asynchscandir( basedir, "" );
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
    }

    /**
     * Scans the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param vpath the path part
     */
    private void asynchscandir( File dir, String vpath ){
        List elementsFound = new ArrayList();
        scandir(  dir, vpath, elementsFound );
        pipelineApi.addElements(  elementsFound );
        if (threadsStarted.decrementAndGet() == 0){
            pipelineApi.addElement( POISON );
        }
    }



    private void scandir( File dir, String vpath, List elementsFound )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception
             *     then???)
             */

            /*
            * [jdcasey] (2) is apparently happening to me, as this is killing one of my tests...
            * this is affecting the assembly plugin, fwiw. I will initialize the newfiles array as
            * zero-length for now.
            *
            * NOTE: I can't find the problematic code, as it appears to come from a native method
            * in UnixFileSystem...
            */
            /*
             * [bentmann] A null array will also be returned from list() on NTFS when dir refers to a soft link or
             * junction point whose target is not existent.
             */
            newfiles = new String[0];

            // throw new IOException( "IO error scanning directory " + dir.getAbsolutePath() );
        }

        File firstDir = null;
        String firstName = null;
        for ( int i = 0; i < newfiles.length; i++ )
        {
            String name = vpath + newfiles[i];
            File file = new File( dir, newfiles[i] );
            if ( file.isFile() )
            {
                if ( isIncluded( name ) )
                {
                    if ( !isExcluded( name ) )
                    {
                        elementsFound.add( name );
                    }
                }
            }
            else if ( file.isDirectory() )
            {
                final boolean couldHoldIncluded = couldHoldIncluded( name );
                if ( isIncluded( name ) || couldHoldIncluded )
                {
                    if ( !isExcluded( name ) || couldHoldIncluded )
                    {
                        if ( firstDir == null )
                        {
                            firstDir = file;
                            firstName = name;
                        }
                        else
                        {
                            final AsynchScanner target = new AsynchScanner( file, name + File.separator );
                            executor.submit(  target );
                            threadsStarted.incrementAndGet();
                        }
                    }
                }
            }
        }
        if ( firstDir != null )
        {
            scandir( firstDir, firstName + File.separator, elementsFound );
        }

    }

    class AsynchScanner implements Runnable {
        File dir;
        String file;

        AsynchScanner( File dir, String file )
        {
            this.dir = dir;
            this.file = file;
        }

        public void run()
        {
            asynchscandir( dir, file);
        }
    }
    private String[] getIncludes( String[] includes )
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

    private String[] getExcludes( String[] excludes )
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

    /**
     * Normalizes the pattern, e.g. converts forward and backward slashes to the platform-specific file separator.
     *
     * @param pattern The pattern to normalize, must not be <code>null</code>.
     * @return The normalized pattern, never <code>null</code>.
     */
    private String normalizePattern( String pattern )
    {
        pattern = pattern.trim();

        if ( pattern.startsWith( org.rosenvold.exp.SelectorUtils.REGEX_HANDLER_PREFIX ) )
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

    /**
     * Tests whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded( String name )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( matchPath( includes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded( String name )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( matchPatternStart( includes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded( String name )
    {
        for ( int i = 0; i < excludes.length; i++ )
        {
            if ( matchPath( excludes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }
}
