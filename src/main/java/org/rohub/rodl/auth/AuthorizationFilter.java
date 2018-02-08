package org.rohub.rodl.auth;



import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public abstract class AuthorizationFilter implements ContainerRequestFilter {

	private static final Logger LOGGER = Logger.getLogger(AuthorizationFilter.class);

	protected static String AUTH_MESSAGE = "AuthMessage";
	
	protected static String BEARER = "Bearer";
	
	/** authentication realm. */
    public static final String REALM = "ROSRS";

    /** URI info. */
    @Context
    protected UriInfo uriInfo;

    /** HTTP request. */
    @Context
    protected HttpServletRequest httpRequest;
    
    
	@Override
	public ContainerRequest filter(ContainerRequest request) {
			
		if(httpRequest.getAttribute("Builder") != null){
			LOGGER.debug("Authorized by previous filter");
			return request;
		}
    	if(authorizationRequired(request)){
    		LOGGER.info("Authorization required");
    		String token = getBearerToken(request);
    		if(isValid(token)){
    			UserMetadata userProfile = getUserProfile(token);
    			if(userProfile != null) {
    				httpRequest.setAttribute("Builder", new Builder(userProfile));
    			}
    		}
    		
    	} else {
    		LOGGER.info("Public");
    		httpRequest.setAttribute("Builder", new Builder(UserProfile.PUBLIC));
    	}
        	
		return request;
	}
	
	protected boolean authorizationRequired(ContainerRequest request) {
		String authHeader = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
		return (authHeader != null && authHeader.length() != 0);
	}
	
	protected String getBearerToken(ContainerRequest request){
		String authHeader = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
		if (authHeader.startsWith(BEARER)) {
			String token = authHeader.substring(BEARER.length());
			if(token != null){
				return token.trim();
			}
		}
		return null;
	}

	protected abstract boolean isValid(String token);
	
	protected abstract UserMetadata getUserProfile(String token);

}
