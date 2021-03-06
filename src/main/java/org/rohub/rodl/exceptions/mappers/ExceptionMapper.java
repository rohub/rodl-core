
package org.rohub.rodl.exceptions.mappers;

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


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

/**
 * Used for catching exception that were not caught by other mappers and returning
 * <code>500 (Internal Server Error)</code> responses.
 * 
 * @author nowakm
 * 
 */
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ExceptionMapper.class);


    @Override
    public Response toResponse(Exception e) {
        if (e instanceof WebApplicationException) {
            WebApplicationException we = (WebApplicationException) e;
            return we.getResponse();
        }
        LOGGER.warn("Caught exception: " + e.getMessage(), e);
        StringBuilder sb = new StringBuilder("");
        sb.append(e.getMessage());
        sb.append("\n");

        // remove these lines before going live
        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append("\t at ");
            sb.append(ste.toString());
            sb.append("\n");
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).type("text/plain").entity(sb.toString()).build();
    }
}
