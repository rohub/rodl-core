package org.rohub.rodl.auth;


import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.rohub.rodl.ApplicationProperties;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.AccessTokenDAO;
import org.rohub.rodl.dl.UserMetadata;

import com.sun.jersey.spi.container.ContainerRequest;

public class RODLAuthorizationFilter extends AuthorizationFilter {
	
	private static final Logger LOGGER = Logger.getLogger(RODLAuthorizationFilter.class);
	
	public ContainerRequest filter(ContainerRequest request) {
		LOGGER.info("Request to: " + uriInfo.getAbsolutePath() + " | method:  " + request.getMethod());
		return super.filter(request);
	}
	
	@Override
	protected boolean isValid(String token) {
		if(token == null || token.length() == 0){
			this.httpRequest.setAttribute(AUTH_MESSAGE, "Invalid user token");
			return false;
		}
		if (DigestUtils.md5Hex(token).equalsIgnoreCase(ApplicationProperties.getAdminTokenHash())) {
			return true;
		}
		
		AccessTokenDAO accessTokenDAO = new AccessTokenDAO();
        AccessToken accessToken = accessTokenDAO.findByValue(token);
        if(accessToken != null){
        	if(accessToken.expired()){
        		accessTokenDAO.delete(accessToken);
        		this.httpRequest.setAttribute(AUTH_MESSAGE, "Token expired");
                LOGGER.debug("Token expired");
        		return false;
        	} else {
        		return true;
        	}
        }
        
        this.httpRequest.setAttribute(AUTH_MESSAGE, "Invalid user token");
        LOGGER.debug("Invalid user token");
        
		return false;
	}


	@Override
	protected UserMetadata getUserProfile(String token) {
		if (DigestUtils.md5Hex(token).equalsIgnoreCase(ApplicationProperties.getAdminTokenHash())) {
            return UserProfile.ADMIN;
        }
		
		AccessTokenDAO accessTokenDAO = new AccessTokenDAO();
        AccessToken accessToken = accessTokenDAO.findByValue(token);
		if (accessToken != null) {
            accessToken.setLastUsed(new Date());
            accessTokenDAO.save(accessToken);
            return accessToken.getUser();
        }
		
		this.httpRequest.setAttribute(AUTH_MESSAGE, "User profile not found");
		LOGGER.debug("User profile not found");
		return null;
	}
	
}
