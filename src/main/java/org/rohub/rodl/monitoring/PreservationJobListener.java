package org.rohub.rodl.monitoring;


import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

/**
 * An empty preservation listener.
 * 
 * @author piotrekhol
 * 
 */
public class PreservationJobListener extends JobListenerSupport {

    @Override
    public String getName() {
        return "Checksum verification job listener";
    }


    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

    }
}
