package org.rohub.rodl.exceptions.mappers;


import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.rohub.rodl.exceptions.ForbiddenException;

/**
 * <p>
 * Maps <code>ForbiddenException</code> to a HTTP <code>403 (Forbidden)</code> response.
 * </p>
 */
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

	private static final Logger LOGGER = Logger.getLogger(ForbiddenExceptionMapper.class);
	
    @Override
    public Response toResponse(ForbiddenException e) {
    	LOGGER.error("Caught forbidden access excepton " + e.getMessage());
        return Response.status(Status.FORBIDDEN).type("text/plain").entity(e.getMessage()).build();
    }

}
