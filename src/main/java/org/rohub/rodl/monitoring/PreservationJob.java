package org.rohub.rodl.monitoring;


import java.net.URI;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rohub.rodl.ApplicationProperties;
import org.rohub.rodl.db.dao.ResearchObjectPreservationStatusDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.preservation.ResearchObjectPreservationStatus;
import org.rohub.rodl.preservation.Status;

/**
 * This job calculates checksums for all resources of a research object and compares them with the checksums stored in
 * the database. The result is stored in the context.
 * 
 * @author pejot
 * 
 */
public class PreservationJob implements Job {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(PreservationJob.class);

    /** Key for the input data. The value must be a URI. */
    public static final String RESEARCH_OBJECT_URI = "ResearchObjectUri";

    /** Resource model builder. */
    private Builder builder;

    /** PReservation dao. */
    ResearchObjectPreservationStatusDAO dao;


    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        URI researchObjectUri = (URI) context.getMergedJobDataMap().get(RESEARCH_OBJECT_URI);
        boolean started = !HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive();
        if (started) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        }
        try {
            if (builder == null) {
                //FIXME RODL URI should be better
                UserMetadata userMetadata = new UserMetadata("rodl", "RODL decay monitor", Role.ADMIN,
                        URI.create(ApplicationProperties.getContextPath()));
                builder = new Builder(userMetadata);
            }
            dao = new ResearchObjectPreservationStatusDAO();
            ResearchObject researchObject = ResearchObject.get(builder, researchObjectUri);
            LOGGER.debug("Processing " + researchObjectUri.toString() + " in context of dArceo");
            ResearchObjectPreservationStatus status = dao.findById(researchObjectUri.toString());
            if (researchObject != null) {
                if (status == null) {
                    status = new ResearchObjectPreservationStatus(researchObjectUri, Status.NEW);
                    dao.save(status);
                } else if (status.getStatus() == null) {
                    status.setStatus(Status.NEW);
                    dao.save(status);
                }
                switch (status.getStatus()) {
                    case NEW:
                    	LOGGER.warn("Preservation client/service not implmeneted. Status NEW");
                        break;
                    case UPDATED:
                    	LOGGER.warn("Preservation client/service not implmeneted. Status UPDATED");
                        break;
                    case UP_TO_DATE:
                        break;
                    default:
                        break;
                }
            } else {
                status = dao.findById(researchObjectUri.toString());
                if (status != null && status.getStatus() == Status.DELETED) {
                	LOGGER.warn("Preservation client/service not implmeneted. Status DELETED");
                }
            }
            status.setStatus(Status.UP_TO_DATE);
            dao.save(status);
        } catch (Exception e) {
            LOGGER.error("Couldn't preserved " + researchObjectUri);
        } finally {
            if (started) {
                HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            }
        }
    }


    public Builder getBuilder() {
        return builder;
    }


    public void setBuilder(Builder builder) {
        this.builder = builder;
    }

}
