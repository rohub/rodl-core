package org.rohub.rodl.accesscontrol;


import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.accesscontrol.dicts.Role;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;
import org.rohub.rodl.accesscontrol.model.dao.PermissionDAO;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;

/**
 * API for setting Research Object access mode.
 * 
 * @author pejot
 * 
 */
@Path("accesscontrol/modes/")
public class AccessModeResource {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(AccessModeResource.class);
    /** URI info. */
    @Context
    UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;

    /** Access Mode dao. */
    private ModeDAO dao = new ModeDAO();


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMode(AccessMode mode)
            throws BadRequestException {
        //first check permissions
        PermissionDAO permissionDAO = new PermissionDAO();
        UserProfileDAO userDAO = new UserProfileDAO();
        //perhaps he is an admin. 
        //admin can everything
        if (!builder.getUser().getRole().equals(org.rohub.rodl.dl.UserMetadata.Role.ADMIN)) {
            List<Permission> permissions = permissionDAO.findByUserROAndPermission(
                userDAO.findByLogin(builder.getUser().getLogin()), mode.getRo(), Role.OWNER);
            if (permissions.size() == 0) {
                throw new BadRequestException("The given ro doesn't exists or doesn't belong to user");
            } else if (permissions.size() > 1) {
                LOGGER.error("Multiply RO authors detected for" + mode.getRo());
                throw new WebApplicationException(500);
            }
        }
        AccessMode storedMode = dao.findByResearchObject(mode.getRo());
        if (storedMode == null) {
            LOGGER.error("Mode for " + mode.getRo() + " Couldn't be found");
            storedMode = new AccessMode();
            storedMode.setRo(mode.getRo());
        }
        
        ResearchObject researchObject = ResearchObject.get(builder, URI.create(mode.getRo()));
        // delete ro from current index - public or private
        researchObject.deleteIndexAttributes();
        
        storedMode.setMode(mode.getMode());
        dao.save(storedMode);
        
        // add ro to current index - public or private
        researchObject.updateIndexAttributes();
        
/*        
        if(mode.getMode() == Mode.PRIVATE && (storedModeValue == Mode.PUBLIC || storedModeValue == Mode.OPEN)) {
	        ResearchObject researchObject = ResearchObject.get(builder, URI.create(mode.getRo()));
	        researchObject.deleteIndexAttributes();

        }
		if((mode.getMode() == Mode.PUBLIC || mode.getMode() == Mode.OPEN) && storedModeValue == Mode.PRIVATE) {
		    ResearchObject researchObject = ResearchObject.get(builder, URI.create(mode.getRo()));
	        researchObject.updateIndexAttributes();

		}
		
        storedMode.setMode(mode.getMode());
        dao.save(storedMode);
        */
        
        //if storedmode == 0
        storedMode.setUri(uriInfo.getRequestUri().resolve(storedMode.getId().toString()));
        return Response.created(uriInfo.getRequestUri().resolve(storedMode.getId().toString())).entity(storedMode)
                .build();
    }


    @GET
    @Path("{mode_id}/")
    public AccessMode getModeById(@PathParam("mode_id") String mode_id) {
        AccessMode result = dao.findById(Integer.valueOf(mode_id));
        if (result != null) {
            result.setUri(uriInfo.getRequestUri().resolve(result.getId().toString()));
        }
        return result;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccessMode getModeByRo(@QueryParam("ro") String ro) {
        AccessMode result = dao.findByResearchObject(ro);
        if (result != null) {
            result.setUri(uriInfo.getBaseUri().resolve("accesscontrol/modes/").resolve(result.getId().toString()));
        }
        return result;
    }
}
