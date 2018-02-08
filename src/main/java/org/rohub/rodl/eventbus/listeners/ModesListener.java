package org.rohub.rodl.eventbus.listeners;


import java.net.URI;

import org.apache.log4j.Logger;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;
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
public class ModesListener {

    /** Access Control Mode dao. */
    ModeDAO dao;

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(ModesListener.class);


    /**
     * Constructor.
     * 
     * @param eventBus
     *            EventBus instance
     */
    public ModesListener(EventBus eventBus) {
        eventBus.register(this);
        dao = new ModeDAO();
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
        if (dao.findByResearchObject(roUri.toString()) != null) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object " + roUri.toString() + " has already defined mode");
        } else {
            AccessMode mode = new AccessMode();
            mode.setMode(org.rohub.rodl.accesscontrol.dicts.Mode.PUBLIC);
            mode.setRo(roUri.toString());
            dao.save(mode);
        }
    }
    
    @Subscribe
    public void onAfterROSnapshotCreate(ROSnapshotAfterCreateEvent event) {
        URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Snapshot " + roUri.toString() + " has already defined mode");
        } else {
            AccessMode mode = new AccessMode();
            mode.setMode(org.rohub.rodl.accesscontrol.dicts.Mode.PUBLIC);
            mode.setRo(roUri.toString());
            dao.save(mode);
        }
    }
    
    @Subscribe
    public void onAfterROForkCreate(ROForkAfterCreateEvent event) {
        URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Fork " + roUri.toString() + " has already defined mode");
        } else {
            AccessMode mode = new AccessMode();
            mode.setMode(org.rohub.rodl.accesscontrol.dicts.Mode.PUBLIC);
            mode.setRo(roUri.toString());
            dao.save(mode);
        }
    }
    
    @Subscribe
    public void onAfterROArchiveCreate(ROArchiveAfterCreateEvent event) {
        URI roUri = event.getResearchObject().getUri();
        if (dao.findByResearchObject(roUri.toString()) != null) {
            //@TODO this is an error. Think how to handle it.
            LOGGER.error("The Research Object Archive " + roUri.toString() + " has already defined mode");
        } else {
            AccessMode mode = new AccessMode();
            mode.setMode(org.rohub.rodl.accesscontrol.dicts.Mode.PUBLIC);
            mode.setRo(roUri.toString());
            dao.save(mode);
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
        AccessMode mode = dao.findByResearchObject(roUri.toString());
        if (mode != null) {
            dao.delete(mode);
            //@TODO this is an error. Think how to handle it.
        } else {
            LOGGER.error("The Research Object " + roUri.toString() + " doesn't have defined mode");
        }
    }
}
