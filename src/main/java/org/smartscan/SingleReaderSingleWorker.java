package org.smartscan;

import org.mentaqueue.AtomicQueue;
import org.mentaqueue.BatchingQueue;
import org.mentaqueue.util.Builder;
import org.smartscan.api.FastFile;
import org.smartscan.api.FastFileReceiver;
import org.smartscan.tools.ScannerTools;
import org.smartscan.tools.SelectorUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads with multiple threads
 */
public class SingleReaderSingleWorker
    extends ModernBase
{
    final BatchingQueue<StringBuilder> queue = new AtomicQueue<>( 2048, new Builder<StringBuilder>()
    {
        @Override
        public StringBuilder newInstance()
        {
            return new StringBuilder( 1024 );
        }
    } );

    private final AtomicInteger threadsStarted = new AtomicInteger( 1 );

    /**
     * Sole constructor.
     *
     * @noinspection JavaDoc, MethodCanBeVariableArityMethod
     */
    public SingleReaderSingleWorker( File basedir, String[] includes, String[] excludes )
    {
        super( basedir, includes, excludes );
        ScannerTools.verifyBaseDir( basedir );
    }


    @SuppressWarnings( "OverlyNestedMethod" )
    void getScanResult( FastFileReceiver fastFileReceiver )
    {
        StringBuilder item;
        while ( true )
        {
            @SuppressWarnings( "NumericCastThatLosesPrecision" ) int avail = (int) queue.availableToPoll();
            if ( avail > 0 )
            {
                for ( int i = 0; i < avail; i++ )
                {
                    item = queue.poll();
                    if ( item != null )
                    {
                        String name = item.toString();
                        if ( name.equals( POISON ) )
                        {
                            return;
                        }
                        char[][] tokenized = SelectorUtils.tokenizePathToCharArray( name, File.separatorChar, 0 );

                        if ( shouldInclude( tokenized ) )
                        {
                            fastFileReceiver.accept( new FastFile( new File( name ), tokenized ) );
                        }
                    }
                }
                queue.donePolling();
            }
        }
    }

    public Thread scanThreaded()
        throws IllegalStateException, InterruptedException
    {
        Runnable scanner = new Runnable()
        {
            @Override
            public void run()
            {
                scandir2( basedir, new char[0][] );
                StringBuilder sb = queue.nextToDispatch();
                while ( sb == null )
                {
                    doSleep( 10 );
                    sb = queue.nextToDispatch();
                }
                sb.setLength( 0 );
                sb.append( POISON );
                queue.flush();
            }
        };

        final Thread thread = new Thread( scanner );
        thread.start();
        return thread;
    }


    protected String normalizeUnicode(String str) {
        Normalizer.Form form = Normalizer.Form.NFD;
        if (!Normalizer.isNormalized(str, form)) {
            return Normalizer.normalize( str, form );
        }
        return str;
    }

    @SuppressWarnings( "AssignmentToMethodParameter" )
    private void scandir2( File parent, char[][] unmodifyableparentvpath )
    {
        File[] newfiles = parent.listFiles();

        if ( newfiles != null )
        {
            char[][] mutablevpath = copyWithOneExtra( unmodifyableparentvpath );
            for ( File file : newfiles )
            {
                mutablevpath[mutablevpath.length - 1] = file.getName().toCharArray();
                BasicFileAttributes basicFileAttributes;
                try
                {
                    basicFileAttributes = Files.readAttributes( file.toPath(), BasicFileAttributes.class );
                }
                catch ( InvalidPathException | IOException e )
                {
                    String a = normalizeUnicode( file.getPath() );
                    String b = file.getPath();
                    System.out.println( "e = " +a + b );
                    continue;
                }
                boolean shouldInclude = shouldInclude( mutablevpath );
                if ( basicFileAttributes.isRegularFile() )
                {
                    if ( shouldInclude )
                    {
                        StringBuilder sb = queue.nextToDispatch();
                        while ( sb == null )
                        {
                            doSleep( 10 );
                            sb = queue.nextToDispatch();
                        }
                        sb.setLength( 0 );
                        sb.append( file.getPath() );
                        queue.flush();
                    }
                }
                else if ( basicFileAttributes.isDirectory() )
                {
                    if ( shouldInclude || couldHoldIncluded( mutablevpath ) )
                    {
                        scandir2( file, copy( mutablevpath ) );
                    }
                }
            }
        }
    }


}
