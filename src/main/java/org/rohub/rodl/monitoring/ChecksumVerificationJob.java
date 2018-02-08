package org.rohub.rodl.monitoring;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rohub.rodl.ApplicationProperties;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.ORE.AggregatedResource;
import org.rohub.rodl.model.RO.ResearchObject;

/**
 * This job calculates checksums for all resources of a research object and compares them with the checksums stored in
 * the database. The result is stored in the context.
 * 
 * @author piotrekhol
 * 
 */
public class ChecksumVerificationJob implements Job {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(ChecksumVerificationJob.class);

    /** Key for the input data. The value must be a URI. */
    public static final String RESEARCH_OBJECT_URI = "ResearchObjectUri";

    /** Resource model builder. */
    private Builder builder;


    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        boolean started = !HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive();
        if (started) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        }
        try {
            URI researchObjectUri = (URI) context.getMergedJobDataMap().get(RESEARCH_OBJECT_URI);
            if (builder == null) {
                //FIXME RODL URI should be better
                UserMetadata userMetadata = new UserMetadata("rodl", "RODL decay monitor", Role.ADMIN,
                        URI.create(ApplicationProperties.getContextPath()));
                builder = new Builder(userMetadata);
            }
            LOGGER.debug("Starting to check mismatches for RO: " + researchObjectUri);
            ResearchObject researchObject = ResearchObject.get(builder, researchObjectUri);
            if (researchObject != null) {
                Result result = new Result(researchObject);
                for (AggregatedResource resource : researchObject.getAggregatedResources().values()) {
                    if (resource.isInternal() && resource.getStats() != null) {
                        String checksumStored = resource.getStats().getChecksum();
                        String checksumCalculated;
                        try (InputStream in = resource.getSerialization()) {
                            checksumCalculated = DigestUtils.md5Hex(in);
                        } catch (IOException e) {
                            LOGGER.error("Can't calculate checksum for " + resource, e);
                            continue;
                        }
                        if (!checksumCalculated.equalsIgnoreCase(checksumStored)) {
                            result.getMismatches()
                                    .add(
                                        new Mismatch(resource.getUri(), resource.getPath(), checksumStored,
                                                checksumCalculated));
                            LOGGER.debug("Detected a mismatch for: " + resource);
                            // save the new checksum
                            String checksumStored2 = resource.updateStats().getChecksum();
                            if (!checksumStored2.equalsIgnoreCase(checksumCalculated)) {
                                LOGGER.error(String
                                        .format(
                                            "The new checksum is still different than the calculated one. Resource = %s, old by DL = %s, calculated = %s, new by DL = %s",
                                            resource, checksumStored, checksumCalculated, checksumStored2));
                            }
                        }
                    }
                    context.setResult(result);
                }
            } else {
                LOGGER.debug("Research Object not found");
            }
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


    /**
     * A checksum verification job result.
     * 
     * @author piotrekhol
     * 
     */
    class Result {

        /** RO. */
        private final ResearchObject researchObject;

        /** A set of differences in checksums expected and calculated. */
        private final Set<Mismatch> mismatches = new HashSet<>();


        /**
         * Constructor.
         * 
         * @param researchObject
         *            RO
         */
        public Result(ResearchObject researchObject) {
            this.researchObject = researchObject;
        }


        public ResearchObject getResearchObject() {
            return researchObject;
        }


        /**
         * True if there are no mismatches in the resources aggregated by the RO.
         * 
         * @return true if there are no mismatches, false otherwise
         */
        public boolean matches() {
            return mismatches.isEmpty();
        }


        public Collection<Mismatch> getMismatches() {
            return mismatches;
        }

    }


    /**
     * A difference in checksum for a resource.
     * 
     * @author piotrekhol
     * 
     */
    public class Mismatch {

        /** Resource URI. */
        private final URI resourceUri;

        /** Resource path relative to RO. */
        private final String resourcePath;

        /** Checksum that was expected. */
        private final String expectedChecksum;

        /** Checksum that was calculated. */
        private final String calculatedChecksum;


        /**
         * Constructor.
         * 
         * @param resourceUri
         *            resource URI
         * @param resourcePath
         *            resource path relative to RO
         * @param expectedChecksum
         *            checksum that was expected
         * @param calculatedChecksum
         *            checksum that was calculated
         */
        public Mismatch(URI resourceUri, String resourcePath, String expectedChecksum, String calculatedChecksum) {
            this.resourceUri = resourceUri;
            this.resourcePath = resourcePath;
            this.expectedChecksum = expectedChecksum;
            this.calculatedChecksum = calculatedChecksum;
        }


        public URI getResourceUri() {
            return resourceUri;
        }


        public String getResourcePath() {
            return resourcePath;
        }


        public String getExpectedChecksum() {
            return expectedChecksum;
        }


        public String getCalculatedChecksum() {
            return calculatedChecksum;
        }
    }
}
