package org.rohub.rodl.model.ROEVO;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.rohub.rodl.db.ResearchObjectId;
import org.rohub.rodl.db.dao.ResearchObjectIdDAO;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.eventbus.events.ROAfterUpdateEvent;
import org.rohub.rodl.evo.EvoType;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.EvoBuilder;
import org.rohub.rodl.model.AO.Annotation;
import org.rohub.rodl.model.RO.Folder;
import org.rohub.rodl.model.RO.Manifest;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.vocabulary.PROV;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;

/**
 * An immutable a research object, i.e. a snapshot or an archive.
 * 
 * The immutable research object can be compared to other immutable research objects, which is implemented by comparing
 * the snapshot/archive dates.
 * 
 * @author piotrekhol
 * 
 */
public class ImmutableResearchObject extends ResearchObject implements Comparable<ImmutableResearchObject> {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ImmutableResearchObject.class);
    /** Evo info annotation. */
    private ImmutableEvoInfo evoInfo;


    /**
     * Constructor.
     * 
     * @param user
     *            user creating the instance
     * @param dataset
     *            custom dataset
     * @param useTransactions
     *            should transactions be used. Note that not using transactions on a dataset which already uses
     *            transactions may make it unreadable.
     * @param uri
     *            RO URI
     */
    public ImmutableResearchObject(UserMetadata user, Dataset dataset, boolean useTransactions, URI uri) {
        super(user, dataset, useTransactions, uri);
    }


    /**
     * Create a new immutable research object as a copy of a live one. Copies all aggregated resources, changes URIs in
     * annotation bodies.
     * 
     * @param uri
     *            URI of the copy
     * @param researchObject
     *            live research object
     * @param builder
     *            model instance builder
     * @param evoType
     *            evolution type
     * @return the new research object
     */
    public static ImmutableResearchObject create(URI uri, ResearchObject researchObject, Builder builder,
            EvoType evoType) {
        ResearchObjectIdDAO idDAO = new ResearchObjectIdDAO();
        uri = idDAO.assignId(new ResearchObjectId(uri)).getId();
        if (ResearchObject.get(builder, uri) != null) {
            throw new ConflictException("Research Object already exists: " + uri);
        }
        DateTime now = DateTime.now();
        ImmutableResearchObject immutableResearchObject = builder.buildImmutableResearchObject(uri,
            researchObject.getCreator(), now);
        immutableResearchObject.setCopyDateTime(now);
        immutableResearchObject.setCopyAuthor(builder.getUser());
        immutableResearchObject.setCopyOf(researchObject);
        EvoBuilder evoBuilder = EvoBuilder.get(evoType);
        immutableResearchObject.copy(researchObject.getManifest(), evoBuilder);
        immutableResearchObject.save(evoType);
        
        // copy the ro:Resources
        LOGGER.debug("start copying resources: " + DateTime.now());
        
        for (org.rohub.rodl.model.RO.Resource resource : researchObject.getResources().values()) {
            try {
            	LOGGER.debug("Copy resource: " + resource.getName());
                immutableResearchObject.copy(resource, evoBuilder);
            } catch (BadRequestException e) {
                LOGGER.warn("Failed to copy the resource", e);
            }
        }

        URI evoBodyUri = researchObject.getFixedEvolutionAnnotationBodyUri();
		Collection<Annotation> evoAnnotaions = researchObject.getAnnotationsByBodyUri().get(evoBodyUri);
		
        //copy the annotations
		LOGGER.debug("start copying annotations: " + DateTime.now());
        for (Annotation annotation : researchObject.getAnnotations().values()) {
            try {
            	URI annotationURI = annotation.getUri();
            	boolean copy = true;
            	LOGGER.debug("Copy annotation: " + annotationURI);
            	for(Annotation evoAnn:evoAnnotaions){
            		if(annotationURI.equals(evoAnn.getUri())){
            			copy = false;
            			LOGGER.debug("Copy operation - skipping evolution annotation: " + annotationURI);
            		}
            	}
            	
            	if(copy){
            		immutableResearchObject.copy(annotation, evoBuilder);
            	}
            } catch (BadRequestException e) {
                LOGGER.warn("Failed to copy the annotation", e);
            }
        }
        //sort the folders topologically
        LOGGER.debug("start sorting folders: " + DateTime.now());
        List<Folder> sorted = new ArrayList<>();
        for (Folder folder : researchObject.getFolders().values()) {
            if (!sorted.contains(folder)) {
                sorted.addAll(visit(folder, sorted));
            }
        }
        //copy the folders
        LOGGER.debug("start copying folders: " + DateTime.now());
        for (Folder folder : sorted) {
            immutableResearchObject.copy(folder, evoBuilder);
        }
        
        LiveEvoInfo liveEvoInfo = researchObject.getLiveEvoInfo();
        liveEvoInfo.addImmutable(immutableResearchObject);
        liveEvoInfo.updateHistory();
        
        LOGGER.debug("finished create ImmutableResearchObject: " + DateTime.now());
        return immutableResearchObject;
    }


    /**
     * Get a Research Object if it exists.
     * 
     * @param builder
     *            model instance builder
     * @param uri
     *            uri
     * @return an existing Research Object or null
     */
    public static ImmutableResearchObject get(Builder builder, URI uri) {
        ImmutableResearchObject researchObject = builder.buildImmutableResearchObject(uri);
        if (!researchObject.getManifest().isNamedGraph()) {
            return null;
        }
        return researchObject;
    }


    public EvoType getEvoType() {
        return getEvoInfo().getEvoType();
    }


    public ResearchObject getLiveRO() {
        return (ResearchObject) getCopyOf();
    }


    public boolean isFinalized() {
        return getImmutableEvoInfo().isFinalized();
    }


    public void setFinalized(boolean finalized) {
        getImmutableEvoInfo().setFinalized(finalized);
    }


    public ImmutableEvoInfo getImmutableEvoInfo() {
        if (evoInfo == null) {
            evoInfo = ImmutableEvoInfo.get(builder, getFixedEvolutionAnnotationBodyUri(), this);
            if (evoInfo != null) {
                evoInfo.load();
            }
        }
        return evoInfo;
    }


    @Override
    public EvoInfo getEvoInfo() {
        return getImmutableEvoInfo();
    }


    /**
     * Generate new evolution information, including the evolution annotation. This method will change the live RO evo
     * info only if this property is set (it isn't for not-finalized snapshots/archives).
     * 
     * @param evoType
     *            snapshot or archive
     */
    @Override
    public void createEvoInfo(EvoType evoType) {
        try {
            evoInfo = ImmutableEvoInfo.create(builder, getFixedEvolutionAnnotationBodyUri(), this, evoType);
            this.evoInfoAnnotation = annotate(evoInfo.getUri(), this);
            this.getManifest().serialize();
        } catch (BadRequestException e) {
            LOGGER.error("Failed to create the evo info annotation", e);
        }
    }


    public void copy(Manifest manifest, EvoBuilder evoBuilder) {
        manifest = manifest.copy(builder, this);
    }


    @Override
    public int compareTo(ImmutableResearchObject o) {
        if (o == null || o.getCopyDateTime() == null) {
            return 1;
        }
        if (getCopyDateTime() == null) {
            return -1;
        }
        return getCopyDateTime().compareTo(o.getCopyDateTime());
    }


    @Override
    public DateTime getCopyDateTime() {
        if (super.getCopyDateTime() == null) {
            getImmutableEvoInfo();
        }
        return super.getCopyDateTime();
    }

    public void delete() {
    	
    	ImmutableEvoInfo evoInfo = this.getImmutableEvoInfo(); // to load all necessary data
    	
		ResearchObject liveRO = getLiveRO();
		LiveEvoInfo liveEvoInfo = liveRO.getLiveEvoInfo();
		liveEvoInfo.removeImmutable(uri);
		liveEvoInfo.updateHistory();
		
		ImmutableResearchObject previousRO = evoInfo.getPreviousRO();
		LOGGER.debug("previous ro: " + previousRO.getUri());
		if(previousRO != null){
			// there is a history before this immutable ro
			List<String> revisions = getRevisionOfRos();
			for(String sRev:revisions){
				LOGGER.debug("revision: " + sRev);
				try{
					ImmutableResearchObject revision = ImmutableResearchObject.get(builder, new URI(sRev));
					ImmutableEvoInfo revEvoInfo = revision.getImmutableEvoInfo();
					revEvoInfo.load();
					revEvoInfo.save(previousRO);
					revEvoInfo.serialize();
				} catch(URISyntaxException e){
					LOGGER.debug(e.getMessage(), e);
				}
			}
		}
		
    	super.delete();
    	
		builder.getEventBusModule().getEventBus().post(new ROAfterUpdateEvent(liveRO));
		
    }
    
    protected List<String> getRevisionOfRos(){
    	List<String> retList = new ArrayList<String>();
    	
    	dataset.begin(ReadWrite.READ);
    	String sQuery = "select ?s where { "
    			+ " ?s ?p ?o "
    			+ "}";
		try {
	    	ParameterizedSparqlString paramSparql = new ParameterizedSparqlString();
			paramSparql.setCommandText(sQuery);
			paramSparql.setParam("p", PROV.wasRevisionOf);
			paramSparql.setParam("o", this.dataset.getDefaultModel().createResource(this.getUri().toString()));
			
			Query query = QueryFactory.create(paramSparql.asQuery()) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
	
	    	try{
	    		ResultSet rs = qexec.execSelect();
	    		if(rs.hasNext()){
	    			String s = rs.next().getResource("s").getURI();
	    			retList.add(s);
	    		}
	    	} finally {
	    		qexec.close();
	    	}
	    } finally {
			dataset.end();
	    }
		
		return retList;
    }
    
}
