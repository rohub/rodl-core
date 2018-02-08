package org.rohub.rodl.job;


import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * Represents a copying job. It runs in a separate thread.
 * 
 * @author piotrekhol
 */
public class Job extends Thread {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(Job.class);


    /**
     * The job state.
     * 
     * @author piotrekhol
     * 
     */
    public enum State {
        /** The job has started and is running. */
        RUNNING,
        /** The job has finished successfully. */
        DONE,
        /** The job has been cancelled by the user. */
        CANCELLED,
        /** The job has failed gracefully. */
        FAILED,
        /** There has been an unexpected error during conversion. */
        SERVICE_ERROR;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        };
    }


    /** Job UUID. */
    private UUID uuid;

    /** Object holding reference to the job. */
    private JobsContainer container;

    /** Operations to be performed. */
    private Operation[] operations;

    /** Current job status. */
    private JobStatus status;


    /**
     * Constructor.
     * 
     * @param jobUUID
     *            job identifier assigned by its container
     * @param status
     *            job status with run parameters
     * @param container
     *            the object that created this job
     * @param operations
     *            list of operations to perform
     */
    public Job(UUID jobUUID, JobStatus status, JobsContainer container, Operation... operations) {
        this.uuid = jobUUID;
        this.status = status;
        this.status.setState(State.RUNNING);
        this.container = container;
        this.operations = operations;
        setDaemon(true);
    }


    @Override
    public void run() {
        try {
            for (Operation operation : operations) {
                operation.execute(status);
            }
            status.setState(State.DONE);
        } catch (OperationFailedException e) {
            LOG.warn("Operation " + uuid + " failed", e);
            status.setStateAndReason(State.FAILED, e.getMessage());
        } catch (Exception e) {
            LOG.error("Operation " + uuid + " terminated unexpectedly", e);
            status.setStateAndReason(State.SERVICE_ERROR, e.getMessage());
        }
        container.onJobDone(this);
    }


    public UUID getUUID() {
        return uuid;
    }


    public JobStatus getStatus() {
        return status;
    }


    /**
     * Abort the job.
     */
    public void abort() {
        this.interrupt();
    }

}
