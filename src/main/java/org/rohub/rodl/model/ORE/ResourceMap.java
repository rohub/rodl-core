package org.rohub.rodl.model.ORE;


import java.io.InputStream;
import java.net.URI;

import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.dl.AccessDeniedException;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.ResourceMetadata;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.IncorrectModelException;
import org.rohub.rodl.model.RDF.Thing;
import org.rohub.rodl.preservation.model.ResearchObjectComponentSerializable;
import org.rohub.rodl.vocabulary.ORE;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;

/**
 * ore:ResourceMap, including ro:Manifest.
 * 
 * @author piotrekhol
 * 
 */
public abstract class ResourceMap extends Thing implements ResearchObjectComponentSerializable {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ResourceMap.class);

    /** ore:Aggregation described by this resource map. */
    protected Aggregation aggregation;

    /** physical representation metadata. */
    private ResourceMetadata stats;


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
     * @param aggregation
     *            aggregation described by the resource map
     * @param uri
     *            resource URI
     */
    public ResourceMap(UserMetadata user, Dataset dataset, boolean useTransactions, Aggregation aggregation, URI uri) {
        super(user, dataset, useTransactions, uri);
        this.aggregation = aggregation;
    }


    @Override
    public void save()
            throws ConflictException, DigitalLibraryException, AccessDeniedException, NotFoundException {
        super.save();
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            Individual aggregationInd = model.createIndividual(aggregation.getUri().toString(), ORE.Aggregation);
            Individual resourceMapInd = model.createIndividual(uri.toString(), ORE.ResourceMap);
            model.add(aggregationInd, ORE.isDescribedBy, resourceMapInd);
            model.add(resourceMapInd, ORE.describes, aggregationInd);

            saveAuthor((Thing) aggregation);
            saveAuthor(this);
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    /**
     * Delete the resource map from the triple store and the storage.
     */
    @Override
    public void delete() {
        try {
            builder.getDigitalLibrary().deleteFile(getResearchObject().getUri(), getPath());
        } catch (Exception e) {
            LOGGER.error("Can't delete resource map " + this + " from DL, will continue with triplestore", e);
        }
        super.delete();
    }


    /**
     * Add a new aggregated resource and save it.
     * 
     * @param resource
     *            a new aggregated resource
     */
    public void saveAggregatedResource(AggregatedResource resource) {
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            Individual ro = model.getIndividual(aggregation.getUri().toString());
            if (ro == null) {
                throw new IncorrectModelException("Aggregation not found: " + aggregation.getUri());
            }
            Individual resourceR = model.createIndividual(resource.getUri().toString(), ORE.AggregatedResource);
            model.add(ro, ORE.aggregates, resourceR);
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    /**
     * Save a nested aggregation as both ore:AggregatedResource and ore:Aggregation.
     * 
     * @param nestedAggregation
     *            a nested aggregation
     */
    public void saveNestedAggregation(Aggregation nestedAggregation) {
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            Individual ro = model.getIndividual(aggregation.getUri().toString());
            if (ro == null) {
                throw new IncorrectModelException("Aggregation not found: " + aggregation.getUri());
            }
            Individual resourceR = model
                    .createIndividual(nestedAggregation.getUri().toString(), ORE.AggregatedResource);
            resourceR.addRDFType(ORE.Aggregation);
            model.add(ro, ORE.aggregates, resourceR);
            Individual manifestR = model.createIndividual(nestedAggregation.getResourceMap().getUri().toString(),
                ORE.ResourceMap);
            model.add(resourceR, ORE.isDescribedBy, manifestR);

            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    /**
     * Save that this aggregation is aggregated by another.
     * 
     * @param parentAggregation
     *            the parent aggregation
     */
    public void saveIsNestedInAggregation(Aggregation parentAggregation) {
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            Individual ro = model.getIndividual(aggregation.getUri().toString());
            if (ro == null) {
                throw new IncorrectModelException("Aggregation not found: " + aggregation.getUri());
            }
            Individual parentR = model.createIndividual(parentAggregation.getUri().toString(), ORE.Aggregation);
            model.add(ro, ORE.isAggregatedBy, parentR);
            model.add(parentR, ORE.aggregates, ro);
            Individual parentManifestR = model.createIndividual(parentAggregation.getResourceMap().getUri().toString(),
                ORE.ResourceMap);
            model.add(parentR, ORE.isDescribedBy, parentManifestR);
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    /**
     * Add a new proxy and save it.
     * 
     * @param proxy
     *            a new proxy
     */
    public void saveProxy(Proxy proxy) {
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
            com.hp.hpl.jena.rdf.model.Resource aggregationR = model.createResource(aggregation.getUri().toString());
            com.hp.hpl.jena.rdf.model.Resource resourceR = model.createResource(proxy.getProxyFor().getUri()
                    .normalize().toString());
            Individual proxyR = model.createIndividual(proxy.getUri().toString(), ORE.Proxy);
            model.add(proxyR, ORE.proxyIn, aggregationR);
            model.add(proxyR, ORE.proxyFor, resourceR);
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }


    /**
     * Store to disk in RDF/XML format. The format is constant because resource maps are not submitted by clients and
     * the RODL gets to choose their default format.
     * 
     * @throws NotFoundException
     *             could not find the resource in DL
     * @throws DigitalLibraryException
     *             could not connect to the DL
     * @throws AccessDeniedException
     *             access denied when updating data in DL
     */
    public void serialize()
            throws DigitalLibraryException, NotFoundException, AccessDeniedException {
        scheduleToSerialize(getResearchObject().getUri(), RDFFormat.RDFXML);
    }


    public String getPath() {
        return getResearchObject().getUri().relativize(uri).getPath();
    }


    public String getRawPath() {
        return getResearchObject().getUri().relativize(uri).getRawPath();
    }


    @Override
    public ResourceMetadata getStats() {
        if (stats == null) {
            stats = builder.getDigitalLibrary().getFileInfo(getResearchObject().getUri(), getPath());
        }
        return stats;
    }


    public void setStats(ResourceMetadata stats) {
        this.stats = stats;
    }


    @Override
    public InputStream getSerialization() {
        return builder.getDigitalLibrary().getFileContents(getResearchObject().getUri(), getPath());
    }


    @Override
    public boolean isInternal() {
        return true;
    }


    @Override
    public InputStream getPublicGraphAsInputStream(RDFFormat syntax) {
        return getGraphAsInputStream(syntax, aggregation.getUri());
    }

}
