package org.rohub.rodl.eventbus.events;


import org.rohub.rodl.model.RO.ResearchObject;

public class ROArchiveAfterCreateEvent {
	 /** Event reason/subject. */
    private final ResearchObject researchObject;


    /**
     * Constructor.
     * 
     * @param researchObject
     *            reason/subject.
     */
    public ROArchiveAfterCreateEvent(ResearchObject researchObject) {
        this.researchObject = researchObject;
    }

    public ResearchObject getResearchObject() {
        return researchObject;
    }
}
