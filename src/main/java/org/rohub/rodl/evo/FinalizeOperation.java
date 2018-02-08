package org.rohub.rodl.evo;


import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.RodlException;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.Operation;
import org.rohub.rodl.job.OperationFailedException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.ROEVO.ImmutableResearchObject;

/**
 * Finalize research object status transformation.
 * 
 * @author piotrekhol
 * 
 */
public class FinalizeOperation implements Operation {

    /** resource builder. */
    private Builder builder;


    /**
     * Constructor.
     * 
     * @param builder
     *            user calling this operation
     */
    public FinalizeOperation(Builder builder) {
        this.builder = builder;
    }


    @Override
    public void execute(JobStatus status)
            throws OperationFailedException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            if (!(status instanceof CopyJobStatus)) {
                throw new OperationFailedException("Given JobStatus is not a instance of CopyJobStatus");
            }
            CopyJobStatus copyJobStatus = (CopyJobStatus) status;
            if (copyJobStatus.getTarget() == null) {
                throw new OperationFailedException("Target research object must be set");
            }
            if (copyJobStatus.getType() == null || copyJobStatus.getType() == EvoType.LIVE ||
            	copyJobStatus.getType() == EvoType.FORK) {
                throw new OperationFailedException("New type must be a snaphot or archive");
            }
            ImmutableResearchObject immutableResearchObject = ImmutableResearchObject.get(builder,
                copyJobStatus.getTarget());
            if (immutableResearchObject == null) {
                throw new NotFoundException("Research Object not found " + copyJobStatus.getTarget());
            }
            immutableResearchObject.setFinalized(true);
            immutableResearchObject.getEvoInfo().updateHistory();
        } catch (RodlException e) {
            throw new OperationFailedException("Could not generate evo info", e);
        } finally {
            builder.getEventBusModule().commit();
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }
}
