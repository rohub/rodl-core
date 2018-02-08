package org.rohub.rodl.accesscontrol.filters;



import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * Admin Resource Access Control filter.
 * 
 * @author pejot
 * 
 */
public class AdminResourceFilter implements ContainerRequestFilter {

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** HTTP request. */
    @Context
    private HttpServletRequest httpRequest;


    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Builder builder = (Builder) httpRequest.getAttribute("Builder");
        UserMetadata user = builder.getUser();
        if (!user.equals(UserProfile.ADMIN)) {
            throw new ForbiddenException("Only admin user can access admin resource");
        }
        return request;

    }
}
