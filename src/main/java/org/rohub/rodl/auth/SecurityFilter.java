package org.rohub.rodl.auth;



import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.rohub.rodl.ApplicationProperties;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.AccessTokenDAO;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.AuthenticationException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * Authentication and authorization filter.
 * 
 * @author piotrekhol
 * 
 */
public class SecurityFilter implements ContainerRequestFilter {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class);

    /** authentication realm. */
    public static final String REALM = "ROSRS";

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** HTTP request. */
    @Context
    private HttpServletRequest httpRequest;


    @Override
    public ContainerRequest filter(ContainerRequest request) {
        try {
            UserMetadata user = authenticate(request);
            if (user == null) {
                throw new NotFoundException("User profile not found");
            }
            httpRequest.setAttribute("Builder", new Builder(user));
            //TODO in here should go access rights control, based on dLibra for example
            //            if (!request.getMethod().equals("GET") && user.getRole() == UserProfile.Role.PUBLIC) {
            //                throw new AuthenticationException("Only authenticated users can do that.", SecurityFilter.REALM);
            //            }
        } catch (DigitalLibraryException e) {
            throw new MappableContainerException(new AuthenticationException("Incorrect login/password\r\n", REALM));
        }

        return request;
    }


    /**
     * Identify the user making the request.
     * 
     * @param request
     *            HTTP request
     * @return user credentials, UserCredentials.PUBLIC_USER if the request is not authenticated
     */
    private UserMetadata authenticate(ContainerRequest request) {
        //TODO allow only secure https connections
        //		logger.info("Connection secure? " + isSecure());
        LOGGER.info("Request to: " + uriInfo.getAbsolutePath() + " | method:  " + request.getMethod());

        // Extract authentication credentials
        String authentication = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication == null) {
            return UserProfile.PUBLIC;
        }
        try {
            if (authentication.startsWith("Bearer ")) {
                // this is the recommended OAuth 2.0 method
                return getBearerCredentials(authentication.substring("Bearer ".length()));
            } else {
                throw new MappableContainerException(new AuthenticationException(
                        "Only HTTP Basic and OAuth 2.0 Bearer authentications are supported\r\n", REALM));
            }
        } catch (IllegalArgumentException e) {
            throw new MappableContainerException(new AuthenticationException(e.getMessage(), REALM));
        }
    }


    /**
     * Find user credentials for a OAuth Bearer token.
     * 
     * @param tokenValue
     *            access token
     * @return user credentials
     */
    public UserMetadata getBearerCredentials(String tokenValue) {
        if (DigestUtils.md5Hex(tokenValue).equalsIgnoreCase(ApplicationProperties.getAdminTokenHash())) {
            return UserProfile.ADMIN;
        }
        AccessTokenDAO accessTokenDAO = new AccessTokenDAO();
        AccessToken accessToken = accessTokenDAO.findByValue(tokenValue);
        if (accessToken != null) {
            accessToken.setLastUsed(new Date());
            accessTokenDAO.save(accessToken);
            return accessToken.getUser();
        } else {
            throw new MappableContainerException(new AuthenticationException("Incorrect access token\r\n", REALM));
        }
    }


    public boolean isSecure() {
        return "https".equals(uriInfo.getRequestUri().getScheme());
    }

}
