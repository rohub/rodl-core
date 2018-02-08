package org.rohub.rodl.accesscontrol.filters;



import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.accesscontrol.dicts.Mode;
import org.rohub.rodl.accesscontrol.dicts.Role;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;
import org.rohub.rodl.accesscontrol.model.dao.PermissionDAO;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * Access Control Resource Access Control Filter.
 * 
 * @author pejot
 * 
 */
public class AccessControlResourceFilter implements ContainerRequestFilter {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(AccessControlResourceFilter.class);

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** HTTP request. */
    @Context
    private HttpServletRequest httpRequest;

    /** Located userProfile. */
    private UserProfile userProfile;
    /** Permissions dao. */
    private PermissionDAO dao = new PermissionDAO();
    /** Mode DAO. */
    private ModeDAO modeDao = new ModeDAO();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Builder builder = (Builder) httpRequest.getAttribute("Builder");
        UserMetadata user = builder.getUser();
        if (user.equals(UserProfile.ADMIN)) {
            return request;
        }
        UserProfileDAO profileDao = new UserProfileDAO();
        userProfile = profileDao.findByLogin(user.getLogin());
        if (userProfile == null) {
            throw new ForbiddenException("The user isn't registered.");
        }
        //there are several action handled by accesscontrol service
        //first discover which component is processed (mode | permission | permissionlink)
        //permissions
        //only RO author and create/delete permission
        //only author and someone involved can query about permission/
        if (request.getPath().contains("/permissions/")) {
            handlePermissionsRequest(request);
        }
        //romode
        //only RO author can change the mode - it's about content so it checked in ro mode resource
        //only RO author can query about mode
        else if (request.getPath().contains("/modes/")) {
            handleROModesRequest(request);
        }
        return request;
    }


    /**
     * Handle RO modes request.
     * 
     * @param request
     *            request
     */
    private void handleROModesRequest(ContainerRequest request) {
        //if this is a POST everybody can do it it's a matter of logic to store object or not
        if (request.getMethod().equals("POST")) {
            return;
        }
        //if there is a GET request to get particular mode, chech if the user is an owner
        if (request.getMethod().equals("GET")) {
            //if there is an ro paramter        	
            if (request.getQueryParameters().getFirst("ro") != null) {
            	String roUri = request.getQueryParameters().getFirst("ro");
               	AccessMode mode = modeDao.findByResearchObject((roUri));
             	//check if it's open then everybody can read
               	if(mode.getMode().equals(Mode.OPEN)) {
            		return;
            	}
                List<Permission> permissions = dao.findByUserROAndPermission(userProfile, roUri, Role.OWNER);
                if (permissions.size() == 1) {
                    return;
                } else if (permissions.size() == 0) {
                    throw new ForbiddenException("This resource doesn't belong to user");
                } else {
                    LOGGER.error("Data problem - more than one owner for " + roUri);
                    throw new WebApplicationException(500);
                }
            } else if (request.getPath().split("modes/").length == 2
                    && isInteger(request.getPath().split("modes/")[1].replace("/", "").replace(" ", ""))) {
                String modeIdString = request.getPath().split("modes/")[1].replace("/", "").replace(" ", "");
                AccessMode mode = modeDao.findById(Integer.valueOf(modeIdString));
              	//check if it's open then everybody can read
               	if(mode.getMode().equals(Mode.OPEN)) {
            		return;
            	}
                List<Permission> permissions = dao.findByUserROAndPermission(userProfile, mode.getRo().toString(), Role.OWNER);
                if (permissions.size() == 1) {
                    return;
                } else if (permissions.size() == 0) {
                    throw new ForbiddenException("This resource doesn't belong to user");
                } else {
                    LOGGER.error("Data problem - more than one owner for " + mode.getUri().toString());
                    throw new WebApplicationException(500);
                }
            }
        }

    }


    /**
     * Handle Permissions request.
     * 
     * @param request
     *            request
     */
    private void handlePermissionsRequest(ContainerRequest request) {
        //if this is a POST everybody can do it it's a matter of logic to store object or not
        if (request.getMethod().equals("POST")) {
            return;
        } else if (request.getMethod().equals("GET")) {
            if (request.getQueryParameters().getFirst("ro") != null) {
                String roUri = request.getQueryParameters().getFirst("ro");
                List<Permission> permissions = dao.findByResearchObject(roUri);
                for (Permission permission : permissions) {
                    //if user is an owner or has this permission granted
                    if (permission.getUser().equals(userProfile)) {
                        return;
                    }
                }
                throw new ForbiddenException("User has no permission to read from this research object");
            } else if (request.getPath().split("permissions/").length == 2
                    && isInteger(request.getPath().split("permissions/")[1])) {
                String permissionIdString = request.getPath().split("permissions/")[1].replace("/", "")
                        .replace(" ", "");
                Permission permission = dao.findById(Integer.valueOf(permissionIdString));
                if (permission == null) {
                    //it will give 404
                    return;
                }
                List<Permission> ownerPermissionList = dao.findByUserROAndPermission(userProfile, permission.getRo(),
                    Role.OWNER);
                Permission ownerPermission = null;
                if (ownerPermissionList.size() == 0) {
                    //it's enough to check user
                    ownerPermission = permission;
                } else if (ownerPermissionList.size() == 1) {
                    ownerPermission = ownerPermissionList.get(0);
                } else if (ownerPermissionList.size() > 1) {
                    LOGGER.error("Data problem - more than one owner for " + permission.getRo());
                    throw new WebApplicationException(500);
                }
                //check resource permissions
                if (!permission.getUser().equals(userProfile) && !ownerPermission.getUser().equals(userProfile)) {
                    throw new ForbiddenException("User has no permission to read from this research object");
                }
            }
        } else if (request.getMethod().equals("DELETE") && request.getPath().split("permissions/").length == 2
                && isInteger(request.getPath().split("permissions/")[1])) {
            //user must be an owner of ro to delete permissions
            String permissionIdString = request.getPath().split("permissions/")[1].replace("/", "").replace(" ", "");
            Permission permission = dao.findById(Integer.valueOf(permissionIdString));
            if (permission == null) {
                //it iwll give 404
                return;
            }
            List<Permission> permissions = dao.findByUserROAndPermission(userProfile, permission.getRo(), Role.OWNER);
            if (permissions.size() == 0) {
                throw new ForbiddenException("This resource doesn't belong to user");
            } else if (permissions.size() > 1) {
                LOGGER.error("Data problem - more than one owner for " + permission.getRo());
                throw new WebApplicationException(500);
            }

        }
    }



    /**
     * If is an integer.
     * 
     * @param s
     *            given string
     * @return true if it is.
     */
    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

}
