package org.rohub.rodl.zip;


import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriInfo;

import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.Operation;
import org.rohub.rodl.job.OperationFailedException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.utils.MemoryZipFile;

/**
 * Operation which stores a research object given in a zip format from outside.
 * 
 * @author pejot
 * 
 */
public class StoreROFromGivenZipOperation implements Operation {

    /** resource builder. */
    private Builder builder;
    /** zip input stream. */
    File zipFile;
    /** request uri info. */
    UriInfo uriInfo;
    /** The zip name. */
    String zipName;


    /**
     * Constructor.
     * 
     * @param builder
     *            model instance builder
     * @param zipFile
     *            processed zip file
     * @param uriInfo
     *            request uri infoz
     * @param zipName
     *            name of the given zip. Used when content of the research object isn't located on the top of the zip
     *            but in the folder
     */
    public StoreROFromGivenZipOperation(Builder builder, File zipFile, UriInfo uriInfo, String zipName) {
        this.builder = builder;
        this.zipFile = zipFile;
        this.uriInfo = uriInfo;
        this.zipName = zipName;
    }


    @Override
    public void execute(JobStatus status)
            throws OperationFailedException {
        if (zipFile == null) {
            throw new OperationFailedException("Givem zip file is empty or null");
        }
        ROFromZipJobStatus roFromZipJobStatus;
        boolean started = !HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive();
        if (started) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        }
        try {
            roFromZipJobStatus = (ROFromZipJobStatus) status;
            URI roUri = roFromZipJobStatus.getTarget();
            try {
                ResearchObject created = ResearchObject.create(builder, roUri, new MemoryZipFile(zipFile, zipName),
                    roFromZipJobStatus);
                roFromZipJobStatus.setProcessedResources(roFromZipJobStatus.getSubmittedResources());
                roFromZipJobStatus.setTarget(created.getUri());
            } catch (IOException | BadRequestException e) {
                throw new OperationFailedException("Can't preapre a ro from given zip", e);
            }
        } finally {
            builder.getEventBusModule().commit();
            if (started) {
                HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            }
            zipFile.delete();
        }
    }
}
