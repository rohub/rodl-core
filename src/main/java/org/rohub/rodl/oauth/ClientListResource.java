
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.OAuthClientList;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.OAuthClientDAO;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import javax.ws.rs.core.UriInfo;

/**
 * OAuth client list REST API resource.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Path("clients")
public class ClientListResource {

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;


    /**
     * Returns list of OAuth clients as XML.
     * 
     * @return An XML serialization of a list of client application DAOs.
     */
    @GET
    @Produces("text/xml")
    public OAuthClientList getClientList() {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage access tokens.");
        }
        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        List<OAuthClient> list = oAuthClientDAO.findAll();
        return new OAuthClientList(list);
    }


    /**
     * Creates new OAuth 2.0 client. input: name and redirection URI.
     * 
     * @param data
     *            text/plain with name in first line and URI in second.
     * @return 201 (Created) when the client was successfully created, 409 (Conflict) if the client id already exists.
     */
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response createClient(String data) {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage access tokens.");
        }
        String[] lines = data.split("[\\r\\n]+");
        if (lines.length < 2) {
            return Response.status(Status.BAD_REQUEST).entity("Content is shorter than 2 lines")
                    .header("Content-type", "text/plain").build();
        }

        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        OAuthClient client = new OAuthClient(lines[0], lines[1]);
        oAuthClientDAO.save(client);

        URI resourceUri = uriInfo.getAbsolutePathBuilder().path("/").build().resolve(client.getClientId());

        return Response.created(resourceUri).build();
    }
}
