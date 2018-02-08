package org.rohub.rodl.eventbus.events;


import org.rohub.rodl.model.RO.ResearchObject;

/**
 * Event thrown once the new Research Object is deleted.
 * 
 * @author pejot
 * 
 */
public class ROAfterDeleteEvent {

    /** Event reason/subject. */
    private final ResearchObject researchObject;


    /**
     * Constructor.
     * 
     * @param researchObject
     *            reason/subject.
     */
    public ROAfterDeleteEvent(ResearchObject researchObject) {
        this.researchObject = researchObject;
    }


    public ResearchObject getResearchObject() {
        return researchObject;
    }
}
