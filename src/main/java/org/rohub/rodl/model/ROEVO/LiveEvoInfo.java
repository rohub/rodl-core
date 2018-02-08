package org.rohub.rodl.model.ROEVO;


import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.evo.EvoType;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.EvoBuilder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.vocabulary.RO;
import org.rohub.rodl.vocabulary.ROEVO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class LiveEvoInfo extends EvoInfo {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(LiveEvoInfo.class);

    private boolean isFork = false;
    
    /** Snapshots or archives of a Live RO, sorted from earliest to most recent. */
    private SortedSet<ImmutableResearchObject> snapshotsOrArchives = new TreeSet<>();


    public LiveEvoInfo(UserMetadata user, Dataset dataset, boolean useTransactions, ResearchObject researchObject,
            URI uri) {
        super(user, dataset, useTransactions, researchObject, uri);
    }


    /**
     * Create and save a new evolution information resource. Add it to the RO properties.
     * 
     * @param builder
     *            model instance builder
     * @param uri
     *            manifest URI
     * @param researchObject
     *            research object that is described
     * @return a new manifest
     */
    public static LiveEvoInfo create(Builder builder, URI uri, ResearchObject researchObject) {
        LiveEvoInfo evoInfo = builder.buildLiveEvoInfo(uri, researchObject, builder.getUser(), DateTime.now());
        evoInfo.save();
        evoInfo.scheduleToSerialize(researchObject.getUri(), RDFFormat.TURTLE);
        researchObject.getAggregatedResources().put(evoInfo.getUri(), evoInfo);
        return evoInfo;
    }


    /**
     * Return an evolution information resource or null if not found in the triplestore.
     * 
     * @param builder
     *            model instance builder
     * @param uri
     *            evolution information resource URI
     * @param researchObject
     *            research object which this evo info should describe
     * @return an existing evo info or null
     */
    public static LiveEvoInfo get(Builder builder, URI uri, ResearchObject researchObject) {
        LiveEvoInfo evoInfo = builder.buildLiveEvoInfo(uri, researchObject, null, null);
        if (!evoInfo.isNamedGraph()) {
            return null;
        }
        researchObject.getAggregatedResources().put(evoInfo.getUri(), evoInfo);
        return evoInfo;
    }


    @Override
    public void save() {
        super.save();
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            Individual ro = model.createIndividual(getResearchObject().getUri().toString(), RO.ResearchObject);
            ro.addRDFType(ROEVO.LiveRO);
            
            if(isFork){
            	ro.addRDFType(ROEVO.ForkedRO);
            }
            for (ImmutableResearchObject immutableRO : snapshotsOrArchives) {
                EvoBuilder builder = EvoBuilder.get(immutableRO.getEvoType());
                builder.saveHasCopy(model, immutableRO);
                builder.saveCopyDateTime(model, immutableRO);
                builder.saveCopyAuthor(model, immutableRO);
            }
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    public SortedSet<ImmutableResearchObject> getImmutableResearchObjects() {
        return snapshotsOrArchives;
    }
    
    public void addImmutable(ImmutableResearchObject ro){
    	this.snapshotsOrArchives.add(ro);
    }
    
    public void removeImmutable(URI immutableURI){
    	
    	Iterator<ImmutableResearchObject> itr = this.snapshotsOrArchives.iterator();
    	ImmutableResearchObject imRO = null;
    	while(itr.hasNext()){
    		ImmutableResearchObject ro = itr.next();
    		if(ro.getUri().equals(immutableURI)){
    			imRO = ro;
    			break;
    		}
    	}
    	
    	if(imRO != null)
    		this.snapshotsOrArchives.remove(imRO);
    	
    	boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
	    	Individual live = model.getIndividual(researchObject.getUri().toString());
	    	Individual immutable = model.getIndividual(immutableURI.toString());
	    	LOGGER.debug("live: " + live + " immutable: " + immutable);
	    	if(immutable != null){
	    		model.removeAll(live, null, immutable);
	    		model.removeAll(immutable, null, null);
	    	}
	        commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    @Override
    public void load() {
        boolean transactionStarted = beginTransaction(ReadWrite.READ);
        try {
            Individual ro = model.getIndividual(getResearchObject().getUri().toString());
            if (ro.hasRDFType(ROEVO.LiveRO)) {
                evoType = EvoType.LIVE;
            } else {
                LOGGER.warn("Evo info has no information about the Live class of this RO: " + getResearchObject());
                evoType = EvoType.LIVE;
            }
            
            if(ro.hasRDFType(ROEVO.ForkedRO)) {
            	isFork = true;
            }
            Set<RDFNode> snapshots = ro.listPropertyValues(ROEVO.hasSnapshot).toSet();
            Set<RDFNode> archives = ro.listPropertyValues(ROEVO.hasArchive).toSet();
            Set<RDFNode> immutables = new HashSet<>();
            immutables.addAll(snapshots);
            immutables.addAll(archives);
            for (RDFNode node : immutables) {
                ImmutableResearchObject immutable = ImmutableResearchObject.get(builder,
                    URI.create(node.asResource().getURI()));
                if (immutable == null) {
                    LOGGER.warn("Immutable research object does not exist: " + node.asResource().getURI());
                } else {
                    snapshotsOrArchives.add(immutable);
                }
            }
        } finally {
            endTransaction(transactionStarted);
        }
    }
    
    public boolean isFork(){
    	return this.isFork;
    }
}
