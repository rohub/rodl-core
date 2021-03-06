
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

import java.io.InputStream;

import org.openrdf.rio.RDFFormat;

/**
 * @author piotrek
 * 
 */
public class QueryResult {

    private final InputStream inputStream;

    private final RDFFormat format;


    /**
     * @param inputStream
     * @param format
     */
    public QueryResult(InputStream inputStream, RDFFormat format) {
        this.inputStream = inputStream;
        this.format = format;
    }


    /**
     * @return the inputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }


    /**
     * @return the format
     */
    public RDFFormat getFormat() {
        return format;
    }

}
