package org.rohub.rodl.eventbus.events;


import org.rohub.rodl.model.RO.ResearchObject;

public class ROSnapshotAfterCreateEvent {
	 /** Event reason/subject. */
    private final ResearchObject researchObject;


    /**
     * Constructor.
     * 
     * @param researchObject
     *            reason/subject.
     */
    public ROSnapshotAfterCreateEvent(ResearchObject researchObject) {
        this.researchObject = researchObject;
    }


    public ResearchObject getResearchObject() {
        return researchObject;
    }
}
