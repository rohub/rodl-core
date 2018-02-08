package org.rohub.rodl.eventbus.listeners;


import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.accesscontrol.model.dao.PermissionDAO;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.eventbus.events.ROAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROAfterDeleteEvent;
import org.rohub.rodl.eventbus.events.ROArchiveAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROForkAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROSnapshotAfterCreateEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Listener for ResearchObject initiate default access control mode for new Research Object.
 * 
 * @author pejot
 * 
 */
public class PermissionsListener {

    /** Access Control Permissions dao. */
    PermissionDAO dao;

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(PermissionsListener.class);


    /**
     * Constructor.
     * 
     * @param eventBus
     *            EventBus instance
     */
    public PermissionsListener(EventBus eventBus) {
        eventBus.register(this);
        dao = new PermissionDAO();
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterROCreate(ROAfterCreateEvent event) {
        URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null && dao.findByResearchObject(roUri.toString()).size() > 0) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object " + roUri.toString() + " has already defined permissions");
        } else {
            Permission permission = new Permission();
            UserProfileDAO userProfileDAO = new UserProfileDAO();
            if (event.getResearchObject().getCreator() != null
                    && userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()) != null) {
                userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin());
                permission.setUser(userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()));
                permission.setRo(roUri.toString());
                permission.setRole(org.rohub.rodl.accesscontrol.dicts.Role.OWNER);
                dao.save(permission);
            } else {
                //@TODO this is an error. Think how to handle it.
                LOGGER.error("The Research Object " + roUri.toString()
                        + " doesn't have a Creator. Can't grant a OWNER role");
            }
        }
    }

    @Subscribe
    public void onAfterROSnapshotCreate(ROSnapshotAfterCreateEvent event) {
    	URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null && dao.findByResearchObject(roUri.toString()).size() > 0) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Snapshot" + roUri.toString() + " has already defined permissions");
        } else {
            Permission permission = new Permission();
            UserProfileDAO userProfileDAO = new UserProfileDAO();
            if (event.getResearchObject().getCreator() != null
                    && userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()) != null) {
                userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin());
                permission.setUser(userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()));
                permission.setRo(roUri.toString());
                permission.setRole(org.rohub.rodl.accesscontrol.dicts.Role.OWNER);
                dao.save(permission);
            } else {
                //@TODO this is an error. Think how to handle it.
                LOGGER.error("The Research Object Snapshot" + roUri.toString()
                        + " doesn't have a Creator. Can't grant a OWNER role");
            }
        }
    }
    
    @Subscribe
    public void onAfterROForkCreate(ROForkAfterCreateEvent event) {
    	URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null && dao.findByResearchObject(roUri.toString()).size() > 0) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Fork" + roUri.toString() + " has already defined permissions");
        } else {
            Permission permission = new Permission();
            UserProfileDAO userProfileDAO = new UserProfileDAO();
            if (event.getResearchObject().getCreator() != null
                    && userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()) != null) {
                userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin());
                permission.setUser(userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()));
                permission.setRo(roUri.toString());
                permission.setRole(org.rohub.rodl.accesscontrol.dicts.Role.OWNER);
                dao.save(permission);
            } else {
                //@TODO this is an error. Think how to handle it.
                LOGGER.error("The Research Object Fork" + roUri.toString()
                        + " doesn't have a Creator. Can't grant a OWNER role");
            }
        }
    }
    
    @Subscribe
    public void onAfterROArchiveCreate(ROArchiveAfterCreateEvent event) {
    	URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null && dao.findByResearchObject(roUri.toString()).size() > 0) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Archive" + roUri.toString() + " has already defined permissions");
        } else {
            Permission permission = new Permission();
            UserProfileDAO userProfileDAO = new UserProfileDAO();
            if (event.getResearchObject().getCreator() != null
                    && userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()) != null) {
                userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin());
                permission.setUser(userProfileDAO.findByLogin(event.getResearchObject().getCreator().getLogin()));
                permission.setRo(roUri.toString());
                permission.setRole(org.rohub.rodl.accesscontrol.dicts.Role.OWNER);
                dao.save(permission);
            } else {
                //@TODO this is an error. Think how to handle it.
                LOGGER.error("The Research Object Archive" + roUri.toString()
                        + " doesn't have a Creator. Can't grant a OWNER role");
            }
        }
    }

    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterRODelete(ROAfterDeleteEvent event) {
        URI roUri = event.getResearchObject().getUri();
        List<Permission> permissions = dao.findByResearchObject(roUri.toString());
        if (permissions != null) {
            for (Permission p : permissions) {
                dao.delete(p);
            }
            //@TODO this is an error. Think how to handle it.
        } else {
            LOGGER.error("The Research Object " + roUri.toString() + " doesn't have any permissions");
        }
    }
}
