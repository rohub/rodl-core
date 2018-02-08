package org.rohub.rodl.job;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.rohub.rodl.job.Job.State;

/**
 * General Job status class.
 * 
 * @author pejot
 * 
 */
@XmlRootElement
public class JobStatus {

    /** job state. */
    protected State state;
    /** Justification of the current state, useful in case of error. */
    protected String reason;
    /** Target RO URI. */
    private URI target;
    
    private Map<String, String> properties;

    /** Constructor. */
    public JobStatus() {
        super();
        this.properties = new HashMap<String, String>();
    }

    public synchronized String getProperty(String name){
    	return this.properties.get(name);
    }

    public synchronized void setProperty(String name, String value){
    	this.properties.put(name, value);
    }

    @XmlElement(name = "status")
    public synchronized State getState() {
        return state;
    }


    public synchronized void setState(State state) {
        this.state = state;
    }


    public synchronized String getReason() {
        return reason;
    }


    public synchronized void setReason(String reason) {
        this.reason = reason;
    }


    public synchronized URI getTarget() {
        return target;
    }


    public synchronized void setTarget(URI target) {
        this.target = target;
    }
    

    /**
     * A synchronized method for setting the job status state.
     * 
     * @param state
     *            state
     * @param message
     *            explanation
     */
    public synchronized void setStateAndReason(State state, String message) {
        this.state = state;
        this.reason = message;
    }

}
