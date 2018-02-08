package org.rohub.rodl.accesscontrol.filters;



import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rohub.rodl.accesscontrol.dicts.Mode;
import org.rohub.rodl.accesscontrol.dicts.Role;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;
import org.rohub.rodl.accesscontrol.model.dao.PermissionDAO;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * RO Resource AccessControl Filter.
 * 
 * @author pejot
 * 
 */
public class ROsResourceFilter implements ContainerRequestFilter {

	/** logger. */
	private static final Logger LOGGER = Logger
			.getLogger(ROsResourceFilter.class);

	/** URI info. */
	@Context
	private UriInfo uriInfo;

	/** HTTP request. */
	@Context
	private HttpServletRequest httpRequest;

	/** Profile DAO. */
	private UserProfileDAO profileDao = new UserProfileDAO();

	/** User Metadata. */
	private UserMetadata user;

	/** User Profile. */
	private UserProfile userProfile;

	/** Access Mode DAO. */
	private ModeDAO modeDao = new ModeDAO();

	/** Permission DAO. */
	private PermissionDAO permissionDAO = new PermissionDAO();

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		LOGGER.info("Request to: " + uriInfo.getAbsolutePath() + " | method:  " + request.getMethod());
		Builder builder = (Builder) httpRequest.getAttribute("Builder");
		user = builder.getUser();
		if (user.equals(UserProfile.ADMIN)) {
			return request;
		}
		if(user.equals(UserProfile.PUBLIC)){
			userProfile = new UserProfile(user.getLogin(), user.getName(), user.getRole());
		} else {
			userProfile = profileDao.findByLogin(user.getLogin());
		}
		
		ROType resourceType = discoverResource(request.getPath(false));
		AccessMode mode = null;
		URI roUri = null;
		
		if (resourceType != ROType.RO_COLLECTION) {
			roUri = getRootROUri(request.getPath(false));
			List<Permission> permissions = permissionDAO
					.findByResearchObject(roUri.toString());
			mode = modeDao.findByResearchObject(roUri.toString());
			if (permissions == null || permissions.size() == 0 || mode == null) {
				LOGGER.warn("Permissions for ro: " + roUri.toString()
						+ " couldn't be calculated.");
				return request;
			} else if (mode.getMode().equals(Mode.OPEN)) {
				return request;
			}
		}

		// handle create operation
		if (request.getMethod().equals("POST")
				&& resourceType == ROType.RO_COLLECTION) {
			// just check if user isn't anonymous to make let him create new ro
			if (user.equals(UserProfile.PUBLIC)) {
				throw new ForbiddenException(
						"User must be logged in to create a new RO");
			} else {
				return request;
			}
		}
		// if user is an author
		if (request.getMethod().equals("GET")
				&& resourceType == ROType.RO_COLLECTION) {
			// not sure what to do...
			// leave to for the app logic
			return request;
		}
		List<Permission> owners = permissionDAO.findByUserROAndPermission(
				userProfile, roUri.toString(), Role.OWNER);
		if (owners != null) {
			if (owners.size() > 1) {
				LOGGER.error("Ro " + roUri + " has more them one owner");
				throw new WebApplicationException(500);
			} else if (owners.size() == 1) {
				// it's an owner, full permissions
				return request;
			}
		}

		// if it's an access to the public resource
		if (request.getMethod().equals("GET")) {
			if (mode.getMode().equals(Mode.PUBLIC)) {
				return request;
			}
			//check owner/reader/writer permission
			if (mode.getMode().equals(Mode.PRIVATE)) {
				List<Permission> editors = permissionDAO.findByUserROAndPermission(
						userProfile, roUri.toString(), Role.EDITOR);
				if (editors.size() > 0) {
					return request;
				}
				List<Permission> readers = permissionDAO.findByUserROAndPermission(
						userProfile, roUri.toString(), Role.READER);
				if (readers.size() > 0) {
					return request;
				}

				if (owners.size() > 0) {
					return request;
				}
				throw new NotFoundException("No resource found");
			}
		}
		// if there is edit request (POST,PUT,DELETE) chec if user has a writer
		// permission

		// exception only author can delete
		if (request.getMethod().equals("DELETE") && isRO(request.getPath())) {
			throw new ForbiddenException("Only an owner can delet whole RO");
		}

		if (request.getMethod().equals("POST")
				|| request.getMethod().equals("DELETE")
				|| request.getMethod().equals("PUT")) {
			List<Permission> editors = permissionDAO.findByUserROAndPermission(
					userProfile, roUri.toString(), Role.EDITOR);
			if (editors != null && editors.size() > 1) {
				LOGGER.warn("There in a duplicated permission for the user "
						+ userProfile.getLogin() + " and ro "
						+ roUri.toString());
				return request;
			}
			if (editors != null && editors.size() == 1) {
				return request;
			} else {
				throw new ForbiddenException("User " + userProfile.getLogin()
						+ " doesn't have permission to modify "
						+ roUri.toString());
			}

		}
		return request;
	}

	private boolean isRO(String path) {
		String[] requestPathArray = path.split("ROs/");
		if (requestPathArray.length == 0) {
			return false;
		}
		String[] resourcePath = requestPathArray[1].split("/");
		if (resourcePath.length == 1) {
			return true;
		}
		return false;
	}

	private ROType discoverResource(String path) {
		String[] requestPathArray = path.split("ROs");
		if (requestPathArray.length == 0) {
			return ROType.RO_COLLECTION;
		}
		String resourcePath = requestPathArray[1];
		if (resourcePath.replace("/", "").equals("")
				&& StringUtils.countMatches(resourcePath, "/") == 1) {
			return ROType.RO_COLLECTION;
		}
		return ROType.RESOURCE;
	}

	enum ROType {
		RESOURCE, RO_COLLECTION
	}

	private URI getRootROUri(String path) {
		String base = uriInfo.getBaseUriBuilder().path("ROs/").build()
				.toString();
		String resource = path.split("ROs/")[1];

		if (resource.split("/").length == 1) {
			return URI.create(base + resource);
		} else {
			return URI.create(base + resource.split("/")[0] + "/");
		}
	}

}
