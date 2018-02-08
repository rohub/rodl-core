
package org.rohub.rodl.sparql;

/*-
 * #%L
 * ROHUB
 * %%
 * Copyright (C) 2010 - 2018 PSNC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.net.URI;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rohub.rodl.sparql.RO_TurtleWriter;

/**
 * @author piotrhol
 * 
 */
public class RO_TurtleWriterTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass()
            throws Exception {
    }


    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass()
            throws Exception {
    }


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
            throws Exception {
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
            throws Exception {
    }


    /**
     * Test method for {@link org.rohub.rodl.sparql.RO_RDFXMLWriter#relativize(java.lang.String)}.
     */
    @Test
    public final void testRelativizeString() {
        RO_TurtleWriter writer = new RO_TurtleWriter();
        writer.setResearchObjectURI(URI.create("http://example.org/ROs/ro1/"));
        writer.setBaseURI(URI.create("http://example.org/ROs/ro1/base/"));

        Assert.assertEquals("<http://example.org/ROs/ro2/resource>",
            writer.formatURI("http://example.org/ROs/ro2/resource"));
        Assert.assertEquals("<resource>", writer.formatURI("http://example.org/ROs/ro1/base/resource"));
        Assert.assertEquals("<../resource>", writer.formatURI("http://example.org/ROs/ro1/resource"));
        Assert.assertEquals("<folder/resource>", writer.formatURI("http://example.org/ROs/ro1/base/folder/resource"));
        Assert.assertEquals("<../folder/resource>", writer.formatURI("http://example.org/ROs/ro1/folder/resource"));

        Assert.assertEquals("<../graph1>", writer.formatURI("http://example.org/ROs/ro1/graph1"));
        Assert.assertEquals("<folder/graph%202>", writer.formatURI("http://example.org/ROs/ro1/base/folder/graph%202"));

        Assert.assertEquals("<resource%20with%20spaces.txt>",
            writer.formatURI("http://example.org/ROs/ro1/base/resource%20with%20spaces.txt"));
    }

}
