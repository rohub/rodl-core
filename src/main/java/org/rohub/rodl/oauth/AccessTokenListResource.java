
package org.rohub.rodl.oauth;

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
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.AccessTokenList;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.AccessTokenDAO;
import org.rohub.rodl.db.dao.OAuthClientDAO;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

/**
 * REST API access tokens resource.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Path("accesstokens")
public class AccessTokenListResource {

    /** logger. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(AccessTokenListResource.class);

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;


    /**
     * Returns list of access tokens as XML. The optional parameters are client_id and user_id.
     * 
     * @param clientId
     *            client application id
     * @param userId
     *            Base64, url-safe encoded.
     * @return an access token XML encoded
     */
    @GET
    @Produces("text/xml")
    public AccessTokenList getAccessTokenList(@QueryParam("client_id") String clientId,
            @QueryParam("user_id") String userId) {
        if (builder.getUser().getRole() != UserMetadata.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage access tokens.");
        }
        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        OAuthClient client = clientId != null ? oAuthClientDAO.findById(clientId) : null;
        UserProfileDAO userProfileDAO = new UserProfileDAO();
        UserProfile userProfile = userId != null ? userProfileDAO.findByLogin(userId) : null;
        AccessTokenDAO accessTokenDAO = new AccessTokenDAO();
        List<AccessToken> list = accessTokenDAO.findByClientOrUser(client, userProfile);
        return new AccessTokenList(list);
    }


    /**
     * Creates new access token for a given client and user. input: client_id and user.
     * 
     * @param data
     *            text/plain with id in first line and password in second.
     * @return 201 (Created) when the access token was successfully created, 400 (Bad Request) if the user does not
     *         exist
     * @throws BadRequestException
     *             the body is incorrect
     */
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response createAccessToken(String data)
            throws BadRequestException {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage access tokens.");
        }
        String[] lines = data.split("[\\r\\n]+");
        if (lines.length < 2) {
            throw new BadRequestException("Content is shorter than 2 lines");
        }

        try {
            OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
            OAuthClient client = oAuthClientDAO.findById(lines[0]);
            if (client == null) {
                throw new BadRequestException("Client not found");
            }
            UserProfileDAO dao = new UserProfileDAO();
            UserProfile creds = dao.findByLogin(lines[1]);
            if (creds == null) {
                throw new BadRequestException("User not found");
            }
            AccessTokenDAO accessTokenDAO = new AccessTokenDAO();
            AccessToken accessToken = new AccessToken(client, creds);
            accessTokenDAO.save(accessToken);
            URI resourceUri = uriInfo.getAbsolutePathBuilder().path("/").build().resolve(accessToken.getToken());

            return Response.created(resourceUri).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.NOT_FOUND).type("text/plain").entity(e.getMessage()).build();
        }
    }
}
