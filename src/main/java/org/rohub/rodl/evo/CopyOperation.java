package org.rohub.rodl.evo;


import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.RodlException;
import org.rohub.rodl.eventbus.events.ROAfterUpdateEvent;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.Operation;
import org.rohub.rodl.job.OperationFailedException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.model.ROEVO.ImmutableResearchObject;

/**
 * Copy one research object to another.
 * 
 * @author piotrekhol
 * 
 */
public class CopyOperation implements Operation {

    /** logger. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(CopyOperation.class);

    /** resource builder. */
    private Builder builder;
    
    private EvoType evoType;


    /**
     * Constructor.
     * 
     * @param builder
     *            model instance builder
     */
    public CopyOperation(Builder builder, EvoType type) {
        this.builder = builder;
        this.evoType = type;
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
            ResearchObject sourceRO = ResearchObject.get(builder, copyJobStatus.getCopyfrom());
            if (sourceRO == null) {
                throw new OperationFailedException("source Research Object does not exist");
            }
            
            EvoType sourceEvoType = sourceRO.checkEvoType();
            if(sourceEvoType != EvoType.LIVE){
            	throw new OperationFailedException(
            			"source Reserach Object evolution type is: " + sourceEvoType + ", must be: LIVE");
            }
            
            try {
            	ResearchObject created = null;
            	if(this.evoType == EvoType.FORK){
            		created = ResearchObject.createFork(copyJobStatus.getTarget(), sourceRO,
    	                    builder, copyJobStatus.getType());
            		
            		builder.getEventBusModule().getEventBus().post(new ROAfterUpdateEvent(sourceRO));
            		
            	} else {
	                created = ImmutableResearchObject.create(copyJobStatus.getTarget(), sourceRO,
	                    builder, copyJobStatus.getType());
	                
	                if(this.evoType == EvoType.ARCHIVE){
	                	builder.getEventBusModule().getEventBus().post(new ROAfterUpdateEvent(sourceRO));
	                } else if(this.evoType == EvoType.SNAPSHOT){
	                	builder.getEventBusModule().getEventBus().post(new ROAfterUpdateEvent(sourceRO));
	                }
	                
            	}
            	
                status.setTarget(created.getUri());
            } catch (RodlException e) {
                throw new OperationFailedException("Failed to copy RO", e);
			} catch (BadRequestException e) {
				throw new OperationFailedException("Failed to copy RO", e);
			} catch (URISyntaxException e) {
				throw new OperationFailedException("Failed to copy RO", e);
			}
        } finally {
            builder.getEventBusModule().commit();
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }

    }
    
   
    
    
}
