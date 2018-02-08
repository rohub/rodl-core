package org.rohub.rodl.oauth;



import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.OAuthClientDAO;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.api.NotFoundException;

/**
 * Client application REST API resource.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Path(("clients" + "/{C_ID}"))
public class ClientResource {

    /** URI info. */
    @Context
    UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;


    /**
     * Get the OAuth 2.0 client.
     * 
     * @param clientId
     *            client id
     * @return an XML serialization of the client DAO
     */
    @GET
    public OAuthClient getClient(@PathParam("C_ID") String clientId) {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage clients.");
        }

        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        return oAuthClientDAO.findById(clientId);
    }


    /**
     * Deletes the OAuth 2.0 client.
     * 
     * @param clientId
     *            client id
     */
    @DELETE
    public void deleteClient(@PathParam("C_ID") String clientId) {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can manage clients.");
        }

        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        OAuthClient client = oAuthClientDAO.findById(clientId);
        if (client == null) {
            throw new NotFoundException();
        }
        oAuthClientDAO.delete(client);
    }
}
