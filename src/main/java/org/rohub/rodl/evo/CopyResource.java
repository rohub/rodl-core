package org.rohub.rodl.evo;



import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.ResearchObjectId;
import org.rohub.rodl.db.dao.ResearchObjectIdDAO;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.job.Job;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.JobsContainer;
import org.rohub.rodl.job.Operation;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;

import com.sun.jersey.api.NotFoundException;

/**
 * The RO copy REST API resource.
 * 
 * @author piotrhol
 * 
 */
@Path("evo/copy/")
public class CopyResource implements JobsContainer {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(CopyResource.class);

    /** Maximum number of concurrent jobs. */
    public static final int MAX_JOBS = 100;

    /** Maximum number of finished jobs kept in memory. */
    public static final int MAX_JOBS_DONE = 100000;

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;

    /** Running jobs. */
    private static Map<UUID, Job> jobs = new ConcurrentHashMap<>(MAX_JOBS);

    /** Statuses of finished jobs. */
    @SuppressWarnings("serial")
    private static Map<UUID, JobStatus> finishedJobs = Collections
            .synchronizedMap(new LinkedHashMap<UUID, JobStatus>() {

                protected boolean removeEldestEntry(Map.Entry<UUID, JobStatus> eldest) {
                    return size() > MAX_JOBS_DONE;
                };
            });

    /** Statuses of finished jobs by target. */
    @SuppressWarnings("serial")
    private static Map<URI, JobStatus> finishedJobsByTarget = Collections
            .synchronizedMap(new LinkedHashMap<URI, JobStatus>() {

                protected boolean removeEldestEntry(Map.Entry<URI, JobStatus> eldest) {
                    return size() > MAX_JOBS_DONE;
                };
            });


    /**
     * Creates a copy of a research object.
     * 
     * @param slug
     *            Slug header
     * @param status
     *            operation parameters
     * @return 201 Created
     * @throws BadRequestException
     *             if the operation parameters are incorrect
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCopyJob(@HeaderParam("Slug") String slug, CopyJobStatus status)
            throws BadRequestException {
        if (status.getCopyfrom() == null) {
            throw new BadRequestException("incorrect or missing \"copyfrom\" attribute");
        }
        if (status.getType() == null) {
            throw new BadRequestException("incorrect or missing \"type\" attribute");
        }
        String id = slug != null ? slug : UUID.randomUUID().toString();
        status.setTarget(uriInfo.getAbsolutePath().resolve("../../ROs/" + id + "/"));
        int i = 1;
        while (ResearchObject.get(builder, status.getTarget()) != null) {
            status.setTarget(uriInfo.getAbsolutePath().resolve("../../ROs/" + id + "-" + (i++) + "/"));
        }
        
        List<Operation> operations = new ArrayList<Operation>();
        
        LOGGER.debug("Target evolution type: " + status.getType());
        
        LOGGER.debug("Requested operations: ");
        CopyOperation copy = new CopyOperation(builder, status.getType());
        operations.add(copy);
        LOGGER.debug("copy");
        
        if(status.isFinalize()){
        	FinalizeOperation finalize = new FinalizeOperation(builder);
        	operations.add(finalize);
        	LOGGER.debug("finalize");
        }
        
        ResearchObjectIdDAO idDAO = new ResearchObjectIdDAO();
        ResearchObjectId firstFree = idDAO.firstFree(new ResearchObjectId(status.getTarget()));
        status.setTarget(firstFree.getId());
        
        UUID jobUUID = UUID.randomUUID();
        Job job = new Job(jobUUID, status, this, operations.toArray(new Operation[operations.size()]));
        jobs.put(jobUUID, job);
        job.start();
        return Response.created(uriInfo.getAbsolutePath().resolve(jobUUID.toString())).entity(job.getStatus()).build();
    }


    @Override
    public void onJobDone(Job job) {
        finishedJobs.put(job.getUUID(), job.getStatus());
        finishedJobsByTarget.put(job.getStatus().getTarget(), job.getStatus());
        jobs.remove(job.getUUID());
    }


    /**
     * Retrieve the job status.
     * 
     * @param uuid
     *            job id
     * @return job status
     */
    @GET
    @Path("{id}")
    public JobStatus getJob(@PathParam("id") UUID uuid) {
        if (jobs.containsKey(uuid)) {
            return jobs.get(uuid).getStatus();
        }
        if (finishedJobs.containsKey(uuid)) {
            return finishedJobs.get(uuid);
        }
        throw new NotFoundException("Job not found: " + uuid);
    }


    /**
     * Find the job status.
     * 
     * @param target
     *            target RO URI
     * @return the job status
     */
    public static JobStatus getStatusForTarget(URI target) {
        return finishedJobsByTarget.get(target);
    }
}
