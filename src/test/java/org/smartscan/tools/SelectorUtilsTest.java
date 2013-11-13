package org.smartscan.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class SelectorUtilsTest
{
    @Test
    public void testTokenizeEmptyStringCharArray()
        throws Exception
    {
        String[] chars2 = SelectorUtils.tokenizePathToString( "", "x" );
        assertEquals( 0, chars2.length );

        char[][] chars = SelectorUtils.tokenizePathToCharArray( "", 'x', 0 );
        assertEquals( 0, chars.length);
    }


    @Test
    public void testTokenizeCharArray()
        throws Exception
    {
        char[][] chars = SelectorUtils.tokenizePathToCharArray( "ABCxDEFxEEEx", 'x', 0 );
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EEE", new String(chars[2]));
    }

    @Test
    public void testTokenizeCharArrayNoEndToken()
        throws Exception
    {
        char[][] chars = SelectorUtils.tokenizePathToCharArray( "ABCxDEFxEER", 'x', 0 );
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EER", new String(chars[2]));
    }

    @Test
    public void testTokenizeltiDelim()
        throws Exception
    {
        char[][] chars = SelectorUtils.tokenizePathToCharArray( "xxABCxDEFxEER", 'x', 0 );
        assertEquals( "ABC", new String( chars[0] ) );
        assertEquals("DEF", new String(chars[1]));
        assertEquals("EER", new String(chars[2]));
    }

    @Test
    public void testTokenizeString()
        throws Exception
    {
        String[] chars = SelectorUtils.tokenizePathToString( "ABCxDEFxEEEx", "x" );
        assertEquals( "ABC", chars[0] );
        assertEquals("DEF", chars[1]);
        assertEquals("EEE", chars[2]);
    }
    @Test
    public void testTokenizeStringNoEndToken()
        throws Exception
    {
        String[] chars = SelectorUtils.tokenizePathToString( "ABCxDEFxEER", "x" );
        assertEquals( "ABC", chars[0] );
        assertEquals("DEF", chars[1]);
        assertEquals("EER", chars[2]);
    }

    @Test
    public void testTokenizeltiDelimStr()
        throws Exception
    {
        String[] chars = SelectorUtils.tokenizePathToString( "xxABCxDEFxEER", "x" );
        assertEquals( "ABC", chars[0] );
        assertEquals("DEF", chars[1] );
        assertEquals("EER", chars[2] );
    }

}
