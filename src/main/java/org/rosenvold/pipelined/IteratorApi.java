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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author Kristian Rosenvold
 */
public class IteratorApi
    implements PipelineApi
{
    private final Vector elements = new Vector();

    private volatile boolean done = false;

    public void addElement( String fileName )
    {
        //noinspection StringEquality
        if ( fileName == PipelinedDirectoryScanner.POISON )
        {
            done = true;
        }
        else
        {
            elements.add( fileName );
        }
        synchronized ( elements )
        {
            elements.notify();
        }
    }

    public void addElements( List elements )
    {
        this.elements.addAll(  elements );
    }

    public Iterator iterator()
    {
        return new MyIterator();
    }


    public class MyIterator
        implements Iterator
    {
        private int clientPos = 0;

        public boolean hasNext()
        {
            final boolean has = hasAvailableElement();
            if (has) return true;
            if ( !done )
            {
                synchronized ( elements )
                {
                    try
                    {
                        elements.wait();
                    }
                    catch ( InterruptedException e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }
            return hasAvailableElement();
        }

        boolean hasAvailableElement()
        {
            return clientPos < elements.size();
        }

        public Object next()
        {
            return elements.get( clientPos++ );
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "Not supported" );
        }
    }
}
