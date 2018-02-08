package org.rohub.rodl.eventbus.listeners;


import java.net.URI;

import org.apache.log4j.Logger;
import org.rohub.rodl.db.dao.ResearchObjectPreservationStatusDAO;
import org.rohub.rodl.eventbus.events.ROAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROAfterDeleteEvent;
import org.rohub.rodl.eventbus.events.ROAfterUpdateEvent;
import org.rohub.rodl.eventbus.events.ROComponentAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROComponentAfterDeleteEvent;
import org.rohub.rodl.eventbus.events.ROComponentAfterUpdateEvent;
import org.rohub.rodl.preservation.ResearchObjectPreservationStatus;
import org.rohub.rodl.preservation.Status;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Listener for ResearchObject and ResearchObjectComponent, performs operation on solr indexs.
 * 
 * @author pejot
 * 
 */
public class PreservationListener {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(PreservationListener.class);
    /** Research Object Status DAO. */
    private ResearchObjectPreservationStatusDAO dao = new ResearchObjectPreservationStatusDAO();


    /**
     * Constructor.
     * 
     * @param eventBus
     *            EventBus instance
     */
    public PreservationListener(EventBus eventBus) {
        eventBus.register(this);
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterROComponentCreate(ROComponentAfterCreateEvent event) {
        URI researchObjectUri = event.getResearchObjectComponent().getResearchObject().getUri();
        ResearchObjectPreservationStatus preservationStatus = dao.findById(researchObjectUri.toString());
        if (preservationStatus != null && preservationStatus.getStatus() != null
                && preservationStatus.getStatus() == Status.UP_TO_DATE) {
            preservationStatus.setStatus(Status.UPDATED);
            dao.save(preservationStatus);
        }
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterROComponentDelete(ROComponentAfterDeleteEvent event) {
        URI researchObjectUri = event.getResearchObjectComponent().getResearchObject().getUri();
        ResearchObjectPreservationStatus preservationStatus = dao.findById(researchObjectUri.toString());
        if (preservationStatus != null) {
            if (preservationStatus.getStatus() != null && preservationStatus.getStatus() == Status.UP_TO_DATE) {
                preservationStatus.setStatus(Status.UPDATED);
                dao.save(preservationStatus);
            }
        } else {
            LOGGER.error("Preservation object for " + researchObjectUri.toString() + " doesn't exist");
        }
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterROComponentUpdate(ROComponentAfterUpdateEvent event) {
        URI researchObjectUri = event.getResearchObjectComponent().getResearchObject().getUri();
        ResearchObjectPreservationStatus preservationStatus = dao.findById(researchObjectUri.toString());
        if (preservationStatus != null) {
            if (preservationStatus.getStatus() != null && preservationStatus.getStatus() == Status.UP_TO_DATE) {
                preservationStatus.setStatus(Status.UPDATED);
                dao.save(preservationStatus);
            }
        } else {
            LOGGER.error("Preservation object for " + researchObjectUri.toString() + " doesn't exist");
        }
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterROCreate(ROAfterCreateEvent event) {
        URI researchObjectUri = event.getResearchObject().getUri();
        if (dao.findById(researchObjectUri.toString()) == null) {
            dao.save(new ResearchObjectPreservationStatus(researchObjectUri, Status.NEW));
        } else {
            LOGGER.error("the object " + researchObjectUri.toString()
                    + " has been already creaded. Can't change the status");
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
        URI researchObjectUri = event.getResearchObject().getUri();
        ResearchObjectPreservationStatus preservationStatus = dao.findById(researchObjectUri.toString());
        if (preservationStatus != null) {
            preservationStatus.setStatus(Status.DELETED);
            dao.save(preservationStatus);
        }
    }


    /**
     * Subscription method.
     * 
     * @param event
     *            processed event
     */
    @Subscribe
    public void onAfterUpdate(ROAfterUpdateEvent event) {
        //nth
    }
}
