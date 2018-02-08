package org.rohub.rodl.model.RO;


import java.io.InputStream;
import java.net.URI;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.eventbus.events.ROComponentAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROComponentBeforeCreateEvent;
import org.rohub.rodl.eventbus.events.ROComponentBeforeDeleteEvent;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.EvoBuilder;
import org.rohub.rodl.model.ORE.AggregatedResource;

import com.hp.hpl.jena.query.Dataset;

/**
 * ro:Resource.
 * 
 * @author piotrekhol
 * @author pejot
 */
public class Resource extends AggregatedResource {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Resource.class);


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
     * @param researchObject
     *            The RO it is aggregated by
     * @param uri
     *            resource URI
     */
    public Resource(UserMetadata user, Dataset dataset, boolean useTransactions, ResearchObject researchObject, URI uri) {
        super(user, dataset, useTransactions, researchObject, uri);
    }


    /**
     * Create and save a new ro:Resource.
     * 
     * @param builder
     *            model instance builder
     * @param researchObject
     *            research object that aggregates the resource
     * @param resourceUri
     *            the URI
     * @return the new resource
     */
    public static Resource create(Builder builder, ResearchObject researchObject, URI resourceUri) {
        if (researchObject.isUriUsed(resourceUri)) {
            throw new ConflictException("Resource already exists: " + resourceUri);
        }
        ResourceFactory resourceFactory = new ResourceFactory(builder);
        Resource resource = resourceFactory.buildResource(resourceUri, researchObject, builder.getUser(),
            DateTime.now());
        resource.postEvent(new ROComponentBeforeCreateEvent(resource));
        resource.setProxy(researchObject.addProxy(resource));
        resource.save();
        resource.onCreated();
        resource.postEvent(new ROComponentAfterCreateEvent(resource));
        return resource;
    }


    /**
     * Create and save a new ro:Resource.
     * 
     * @param builder
     *            model instance builder
     * @param researchObject
     *            research object that aggregates the resource
     * @param resourceUri
     *            the URI
     * @param content
     *            the resource content
     * @param contentType
     *            the content MIME type
     * @return the new resource
     * @throws BadRequestException
     *             if it is expected to be an RDF file and isn't
     */
    public static Resource create(Builder builder, ResearchObject researchObject, URI resourceUri, InputStream content,
            String contentType)
            throws BadRequestException {
        if (researchObject.isUriUsed(resourceUri)) {
            throw new ConflictException("Resource already exists: " + resourceUri);
        }
        ResourceFactory resourceFactory = new ResourceFactory(builder);
        Resource resource = resourceFactory.buildResource(resourceUri, researchObject, builder.getUser(),
            DateTime.now(), contentType);
        resource.postEvent(new ROComponentBeforeCreateEvent(resource));
        resource.setProxy(researchObject.addProxy(resource));
        //System.out.println("size: "+content.toString().length()+" contentType: "+contentType.toString());
        resource.save(content, contentType);
        if (researchObject.getAnnotationsByBodyUri().containsKey(resource.getUri())) {
            resource.saveGraphAndSerialize();
        }
        resource.onCreated();
        resource.postEvent(new ROComponentAfterCreateEvent(resource));
        return resource;
    }


    @Override
    protected void onCreated() {
        try {
            super.onCreated();
        } catch (BadRequestException e) {
            LOGGER.error("Unexpected error when post processing the resource", e);
        }
        researchObject.getResources().put(getUri(), this);
    }


    /**
     * Create a new resource with all data except for the URI equal to another resource.
     * 
     * @param builder
     *            model instance builder
     * @param evoBuilder
     *            builder of evolution properties
     * @param researchObject
     *            research object that should aggregate the new resource
     * @return the new resource
     * @throws BadRequestException
     *             if it is expected to be an RDF file and isn't
     */
    public Resource copy(Builder builder, EvoBuilder evoBuilder, ResearchObject researchObject)
            throws BadRequestException {
    	
    	URI resourceUri = researchObject.getUri().resolve(getRawPath());
    	
    	// Try to figure out if resource is internal or external.
    	// External resource will have different domain in URL.
    	// For the external resources, resourceURI should remain the same
    	String roHostName = researchObject.getUri().getHost();
    	String resourceHostName = getUri().getHost();
    	if(!roHostName.equals(resourceHostName)){
    		resourceUri = getUri();
    	}
    	LOGGER.debug("raw path: " + getRawPath());
        LOGGER.debug("resourceURI: " + resourceUri);
        if (researchObject.isUriUsed(resourceUri)) {
            throw new ConflictException("Resource already exists: " + resourceUri);
        }
        ResourceFactory resourceFactory = new ResourceFactory(builder);
        Resource resource2 = resourceFactory.buildResource(resourceUri, researchObject, getCreator(), getCreated());
        resource2.setCopyDateTime(DateTime.now());
        resource2.setCopyAuthor(builder.getUser());
        resource2.setCopyOf(this);
        resource2.setProxy(researchObject.addProxy(resource2));
        if (isInternal()) {
            // backwards compatiblity
            resource2.save(getSerialization(),
                getStats() != null ? getStats().getMimeType() : RDFFormat.forFileName(getName(), RDFFormat.RDFXML)
                        .getDefaultMIMEType());
            if (researchObject.getAnnotationsByBodyUri().containsKey(resource2.getUri())) {
                resource2.saveGraphAndSerialize();
            }
        } else {
            resource2.save();
        }
        resource2.onCreated();
        return resource2;
    }


    @Override
    protected void save() {
        super.save();
        researchObject.getManifest().saveRoResourceClass(this);
        researchObject.getManifest().saveRoStats(this);
    }


    @Override
    public void delete() {
        this.postEvent(new ROComponentBeforeDeleteEvent(this));
        getResearchObject().getResources().remove(uri);
        super.delete();
    }


    @Override
    public void saveGraphAndSerialize()
            throws BadRequestException {
        super.saveGraphAndSerialize();
        //FIXME the resource is still of class Resource, not AggregatedResource
        getResearchObject().getManifest().removeRoResourceClass(this);
        getResearchObject().getResources().remove(uri);
    }

}
