
package org.rohub.rodl.model.RO;

/*-
 * #%L
 * ROHUB
 * %%
 * Copyright (C) 2010 - 2018 PSNC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.accesscontrol.dicts.Mode;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;
import org.rohub.rodl.db.ResearchObjectId;
import org.rohub.rodl.db.dao.ResearchObjectIdDAO;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.ServiceUserMetadata;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.eventbus.events.ROAfterCreateEvent;
import org.rohub.rodl.eventbus.events.ROAfterDeleteEvent;
import org.rohub.rodl.eventbus.events.ROBeforeCreateEvent;
import org.rohub.rodl.eventbus.events.ROBeforeDeleteEvent;
import org.rohub.rodl.eventbus.events.ROForkAfterCreateEvent;
import org.rohub.rodl.evo.EvoType;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.EvoBuilder;
import org.rohub.rodl.model.AO.Annotation;
import org.rohub.rodl.model.ORE.AggregatedResource;
import org.rohub.rodl.model.ORE.Aggregation;
import org.rohub.rodl.model.ORE.Proxy;
import org.rohub.rodl.model.ORE.ResourceMap;
import org.rohub.rodl.model.RDF.Thing;
import org.rohub.rodl.model.ROEVO.EvoInfo;
import org.rohub.rodl.model.ROEVO.ForkEvoInfo;
import org.rohub.rodl.model.ROEVO.ImmutableResearchObject;
import org.rohub.rodl.model.ROEVO.LiveEvoInfo;
import org.rohub.rodl.preservation.model.ResearchObjectComponentSerializable;
import org.rohub.rodl.preservation.model.ResearchObjectSerializable;
import org.rohub.rodl.utils.MemoryZipFile;
import org.rohub.rodl.utils.MimeTypeUtil;
import org.rohub.rodl.vocabulary.ORE;
import org.rohub.rodl.vocabulary.RO;
import org.rohub.rodl.vocabulary.ROEVO;
import org.rohub.rodl.zip.ROFromZipJobStatus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A research object, live by default.
 * 
 * @author piotrekhol
 * 
 */
public class ResearchObject extends Thing implements Aggregation, ResearchObjectSerializable {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ResearchObject.class);

    /** Manifest path. */
    public static final String MANIFEST_PATH = ".ro/manifest.rdf";

    /** Fixed roevo annotation file path. */
    private static final String ROEVO_PATH = ".ro/evo_info.ttl";

    /** aggregated resources, including annotations, resources and folders. */
    protected Map<URI, AggregatedResource> aggregatedResources;

    /** proxies declared in this RO. */
    private Map<URI, Proxy> proxies;

    /** aggregated ro:Resources, excluding ro:Folders. */
    private Map<URI, Resource> resources;

    /** aggregated ro:Folders. */
    private Map<URI, Folder> folders;

    /** aggregated annotations, grouped based on ao:annotatesResource. */
    private Multimap<URI, Annotation> annotationsByTargetUri;

    /** aggregated annotations, grouped based on ao:annotatesResource. */
    private Multimap<URI, Annotation> annotationsByBodyUri;

    /** aggregated annotations. */
    private Map<URI, Annotation> annotations;

    /** folder resource maps and the manifest. */
    private Map<URI, ResourceMap> resourceMaps;

    /** folder entries. */
    private Map<URI, FolderEntry> folderEntries;

    /** folder entries, grouped based on ore:proxyFor. */
    private Multimap<URI, FolderEntry> folderEntriesByResourceUri;

    /** Manifest. */
    private Manifest manifest;

    /** Evolution information annotation body. */
    private LiveEvoInfo evoInfo;

    /** The annotation for the evolution information. */
    protected Annotation evoInfoAnnotation;

    /** Optional URI of this research object bundled as an RO bundle. */
    protected URI bundleUri;

    /** URI of the RO that aggregates this RO, if it is nested. */
    protected Collection<URI> aggregatingROUris;

    private ModeDAO accessModeDao = new ModeDAO();
    
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
     *            the RO URI
     */
    public ResearchObject(UserMetadata user, Dataset dataset, boolean useTransactions, URI uri) {
        super(user, dataset, useTransactions, uri);
    }


    /**
     * Create new Research Object.
     * 
     * @param builder
     *            model instance builder
     * @param uri
     *            RO URI
     * @return an instance
     */
    public static ResearchObject create(Builder builder, URI uri) {
        ResearchObjectIdDAO idDAO = new ResearchObjectIdDAO();
        //replace uri on the first free
        uri = idDAO.assignId(new ResearchObjectId(uri)).getId();
        //because of the line above should never be true;
        if (get(builder, uri) != null) {
            throw new ConflictException("Research Object already exists: " + uri);
        }
        ResearchObject researchObject = builder.buildResearchObject(uri, builder.getUser(), DateTime.now());
        researchObject.manifest = Manifest.create(builder, researchObject.getUri().resolve(MANIFEST_PATH),
            researchObject);
        researchObject.postEvent(new ROBeforeCreateEvent(researchObject));
        researchObject.save(EvoType.LIVE);
        researchObject.postEvent(new ROAfterCreateEvent(researchObject));
        return researchObject;
    }


    /**
     * Create a research object with the given resources, annotations and folders. The resources, annotations and
     * folders are not used directly but only their parameters are used as base.
     * 
     * @param builder
     *            model builder
     * @param uri
     *            RO URI
     * @param resources2
     *            resources to use as base
     * @param annotations2
     *            annotations to use as base
     * @param folders2
     *            folders to use as base
     * @return the new research object
     * @throws BadRequestException
     *             when provided parameters are incorrect
     */
    public static ResearchObject create(Builder builder, URI uri, Collection<? extends Resource> resources2,
            Collection<? extends Annotation> annotations2, Collection<? extends Folder> folders2)
            throws BadRequestException {
        ResearchObject researchObject = create(builder, uri);
        for (Resource resource : resources2) {
            if (resource.isInternal()) {
                Resource resource2 = researchObject.aggregate(resource.getPath(), resource.getSerialization(), resource
                        .getStats().getMimeType());
                LOGGER.debug("Aggregated an internal resource " + resource2);
            } else {
                Resource resource2 = researchObject.aggregate(resource.getUri());
                LOGGER.debug("Aggregated an external resource " + resource2);
            }
        }
        for (Annotation annotation : annotations2) {
            try {
                Set<Thing> targets = new HashSet<>();
                for (Thing target : annotation.getAnnotated()) {
                    targets.add(Annotation.validateTarget(researchObject,
                        researchObject.getUri().resolve(target.getUri())));
                }
                if (annotation.getBody() instanceof AggregatedResource
                        && ((AggregatedResource) annotation.getBody()).isInternal()) {
                    AggregatedResource body = (AggregatedResource) annotation.getBody();
                    try {
                        Resource body2 = researchObject.aggregate(body.getPath(), body.getSerialization(), body
                                .getStats().getMimeType());
                        LOGGER.debug("Aggregated an internal annotation body " + body2);
                    } catch (ConflictException e) {
                        LOGGER.debug("The internal annotation body has already been aggregated " + body.getPath());
                    }
                } else {
                    // external annotation bodies are not aggregated
                    LOGGER.debug("Identified an external annotation body " + annotation.getBody());
                }
                Annotation annotation2 = researchObject.annotate(
                    researchObject.getUri().resolve(annotation.getBody().getUri()), targets);
                LOGGER.debug("Aggregated an annotation with body " + annotation2.getBody().getUri());
            } catch (BadRequestException e) {
                LOGGER.warn("Annotation " + annotation.getUri() + " will be ignored, reason: " + e.getMessage());
            }
        }
        for (Folder folder : folders2) {
            researchObject.aggregateFolder(researchObject.getUri().resolve(folder.getPath()));
            LOGGER.debug("Aggregated folder " + researchObject.getUri().resolve(folder.getPath()));
        }
        for (Entry<URI, Folder> entryFolder : researchObject.getFolders().entrySet()){
        	for (Folder folder : folders2) {
        		if (folder.getPath().equals(entryFolder.getValue().getPath())){
        			for (FolderEntry entry : folder.getFolderEntries().values()) {
                        URI resourceUri = researchObject.getUri().resolve(entry.getProxyFor().getUri());
                        AggregatedResource resource = researchObject.getResources().get(resourceUri); 
                        if (resource == null) {
                        	resource = researchObject.getFolders().get(resourceUri);
                        	if (resource == null) {
                        		LOGGER.warn("Resource for entry not found: " + resourceUri);
                        		continue;
                        	}
                        }
                        entryFolder.getValue().createFolderEntry(resource);
                        LOGGER.debug("Created an entry for " + resource.getUri() + " in " + entryFolder.getValue().getUri());
                    }
        		}
        	}
        }
        return researchObject;
    }


    /**
     * Generate new evolution information, including the evolution annotation.
     * 
     * @param type
     *            evolution type
     */
    protected void createEvoInfo(EvoType type) {
        try {
        	if(type == EvoType.FORK){
        		evoInfo = ForkEvoInfo.create(builder, getFixedEvolutionAnnotationBodyUri(), this);
        	} else {
        		evoInfo = LiveEvoInfo.create(builder, getFixedEvolutionAnnotationBodyUri(), this);
        	}
            this.evoInfoAnnotation = annotate(evoInfo.getUri(), this);
            this.getManifest().serialize();
        } catch (BadRequestException e) {
            LOGGER.error("Failed to create the evo info annotation", e);
        }
    }


    /**
     * Get the resource with the evolution metadata.
     * 
     * @return an evolution resource
     */
    public LiveEvoInfo getLiveEvoInfo() {
        if (evoInfo == null) {
            evoInfo = LiveEvoInfo.get(builder, getFixedEvolutionAnnotationBodyUri(), this);
            if (evoInfo != null) {
                evoInfo.load();
            }
        }
        return evoInfo;
    }


    public Annotation getEvoInfoAnnotation() {
        return evoInfoAnnotation;
    }


    public SortedSet<ImmutableResearchObject> getImmutableResearchObjects() {
        return getLiveEvoInfo().getImmutableResearchObjects();
    }


    /**
     * Get the manifest, loaded lazily.
     * 
     * @return the manifest
     */
    public Manifest getManifest() {
        if (manifest == null) {
            this.manifest = builder.buildManifest(getManifestUri(), this);
        }
        return manifest;
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
    public static ResearchObject get(Builder builder, URI uri) {
        ResearchObject researchObject = builder.buildResearchObject(uri);
        if (researchObject.getManifest().isNamedGraph()) {
            return researchObject;
        } else {
            return null;
        }
    }


    /**
     * Save.
     * 
     * @param evoType
     *            evolution type
     */
    protected void save(EvoType evoType) {
        super.save();
        getManifest().save();
        //TODO check if to create an RO or only serialize the manifest
        builder.getDigitalLibrary().createResearchObject(uri,
            getManifest().getGraphAsInputStreamWithRelativeURIs(uri, RDFFormat.RDFXML), ResearchObject.MANIFEST_PATH,
            RDFFormat.RDFXML.getDefaultMIMEType());
        createEvoInfo(evoType);
    }


    /**
     * Delete the Research Object including its resources and annotations.
     */
    @Override
    public void delete() {
        this.postEvent(new ROBeforeDeleteEvent(this));
        //create another collection to avoid concurrent modification
        Set<AggregatedResource> resourcesToDelete = new HashSet<>(getAggregatedResources().values());
        for (AggregatedResource resource : resourcesToDelete) {
            try {
                resource.delete();
            } catch (Exception e) {
                LOGGER.error("Can't delete resource " + resource + ", will continue deleting the RO.", e);
            }
        }
        if (getBundleUri() != null) {
            try {
                // The bundle may be stored inside the parent RO. The path may then start with ../[parentRO]/.
                Path bundlePath = Paths.get(uri.getPath()).relativize(Paths.get(getBundleUri().getPath()));
                // delete the bundled file
                builder.getDigitalLibrary().deleteFile(uri, bundlePath.toString());
                // delete the references in the manifest
                for (URI parentUri : getAggregatingROUris()) {
                    ResearchObject parent = ResearchObject.get(builder, parentUri);
                    // if the parent RO is being deleted, it may have already deleted the references to this RO
                    if (parent.getAggregatedResources().containsKey(uri)) {
                        ((RoBundle) parent.getAggregatedResources().get(uri)).delete(false);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to delete the bundled version of the RO " + this, e);
            }
        }
        getManifest().delete();
        try {
            builder.getDigitalLibrary().deleteResearchObject(uri);
        } catch (NotFoundException e) {
            // good, nothing was left so the folder was deleted
            LOGGER.debug("As expected. RO folder was empty and was deleted: " + e.getMessage());
        }
        this.postEvent(new ROAfterDeleteEvent(this));
        super.delete();
    }


    /**
     * Create and save a new proxy.
     * 
     * @param resource
     *            resource for which the proxy is
     * @return a proxy instance
     */
    public Proxy addProxy(AggregatedResource resource) {
        URI proxyUri = uri.resolve(".ro/proxies/" + UUID.randomUUID());
        Proxy proxy = builder.buildProxy(proxyUri, resource, this);
        proxy.save();
        getProxies().put(proxy.getUri(), proxy);
        return proxy;
    }


    /**
     * Create an internal resource and add it to the research object.
     * 
     * @param path
     *            resource path, relative to the RO URI, not encoded
     * @param content
     *            resource content
     * @param contentType
     *            resource Content Type
     * @return the resource instance
     * @throws BadRequestException
     *             if it should be an annotation body according to an existing annotation and it's the wrong format
     */
    public Resource aggregate(String path, InputStream content, String contentType)
            throws BadRequestException {
        URI resourceUri = UriBuilder.fromUri(uri).path(path).build();
        Resource resource = Resource.create(builder, this, resourceUri, content, contentType);
        return resource;
    }


    /**
     * Add an external resource (a reference to a resource) to the research object.
     * 
     * @param uri
     *            resource URI
     * @return the resource instance
     */
    public Resource aggregate(URI uri) {
        Resource resource = Resource.create(builder, this, uri);
        return resource;
    }


    /**
     * Aggregate a copy of the resource. The creation date and authors will be taken from the original. The URI of the
     * new resource will be different from the original.
     * 
     * @param resource
     *            the resource to copy
     * @param evoBuilder
     *            builder of evolution properties
     * @return the new resource
     * @throws BadRequestException
     *             if it should be an annotation body according to an existing annotation and it's the wrong format
     */
    public Resource copy(Resource resource, EvoBuilder evoBuilder)
            throws BadRequestException {
        Resource resource2 = resource.copy(builder, evoBuilder, this);
        return resource2;
    }


    /**
     * Aggregate a copy of the resource. The creation date and authors will be taken from the original. The URI of the
     * new resource will be different from the original.
     * 
     * @param resource
     *            the resource to copy
     * @param evoBuilder
     *            builder of evolution properties
     * @return the new resource
     * @throws BadRequestException
     *             if it should be an annotation body according to an existing annotation and it's the wrong format
     */
    public AggregatedResource copy(AggregatedResource resource, EvoBuilder evoBuilder)
            throws BadRequestException {
        AggregatedResource resource2 = resource.copy(builder, evoBuilder, this);
        return resource2;
    }


    /**
     * Add a named graph describing the folder and aggregate it by the RO. The folder must have its URI set. The folder
     * entries must have their proxyFor and RO name set. The folder entry URIs will be generated automatically if not
     * set.
     * 
     * If there is no root folder in the RO, the new folder will be the root folder of the RO.
     * 
     * @param folderUri
     *            folder URI
     * @param content
     *            folder description
     * @return a folder instance
     * @throws BadRequestException
     *             the folder description is not valid
     */
    public Folder aggregateFolder(URI folderUri, InputStream content)
            throws BadRequestException {
        Folder folder = Folder.create(builder, this, folderUri, content);
        return folder;
    }


    /**
     * Create and aggregate an empty folder instance.
     * 
     * @param folderUri
     *            a folderUri
     * @return an empty folder
     */
    public Folder aggregateFolder(URI folderUri) {
        Folder folder = Folder.create(builder, this, folderUri);
        return folder;
    }


    /**
     * Aggregate a copy of a folder. The aggregated resources will be relativized against the original RO URI and
     * resolved against this RO URI.
     * 
     * @param folder
     *            folder to copy
     * @param evoBuilder
     *            builder of evolution properties
     * @return the new folder
     */
    public Folder copy(Folder folder, EvoBuilder evoBuilder) {
        Folder folder2 = folder.copy(builder, evoBuilder, this);
        return folder2;
    }


    /**
     * Add and aggregate a new annotation to the research object.
     * 
     * @param body
     *            annotation body URI
     * @param target
     *            annotated resource's URI
     * @return new annotation
     * @throws BadRequestException
     *             if there is no data in storage or the file format is not RDF
     */
    public Annotation annotate(URI body, Thing target)
            throws BadRequestException {
        return annotate(body, new HashSet<>(new ArrayList<Thing>(Arrays.asList(target))), null);
    }


    /**
     * Add and aggregate a new annotation to the research object.
     * 
     * @param body
     *            annotation body URI
     * @param targets
     *            list of annotated resources URIs
     * @return new annotation
     * @throws BadRequestException
     *             if there is no data in storage or the file format is not RDF
     */
    public Annotation annotate(URI body, Set<Thing> targets)
            throws BadRequestException {
        return annotate(body, targets, null);
    }


    /**
     * Add and aggregate a new annotation to the research object.
     * 
     * @param body
     *            annotation body URI
     * @param targets
     *            list of annotated resources URIs
     * @param annotationId
     *            the id of the annotation, may be null
     * @return new annotation
     * @throws BadRequestException
     *             if there is no data in storage or the file format is not RDF
     */
    public Annotation annotate(URI body, Set<Thing> targets, String annotationId)
            throws BadRequestException {
        URI annotationUri = getAnnotationUri(annotationId);
        Annotation annotation = Annotation.create(builder, this, annotationUri, body, targets);
        return annotation;
    }


    /**
     * Add and aggregate a new annotation to the research object.
     * 
     * @param data
     *            annotation description
     * @return new annotation
     * @throws BadRequestException
     *             if there is no data in storage or the file format is not RDF
     */
    public Annotation annotate(InputStream data)
            throws BadRequestException {
        URI annotationUri = getAnnotationUri(null);
        Annotation annotation = Annotation.create(builder, this, annotationUri, data);
        return annotation;
    }


    /**
     * Create a copy of an annotation and aggregated it. The annotation URI will be different, the other fields will be
     * the same. If the body is aggregated in the original annotation's RO, and it's not aggregated in this RO, then it
     * is also copied.
     * 
     * @param annotation
     *            the annotation to copy
     * @param evoBuilder
     *            builder of evolution properties
     * @return the new annotation
     * @throws BadRequestException
     *             if there is no data in storage or the file format is not RDF
     */
    public Annotation copy(Annotation annotation, EvoBuilder evoBuilder)
            throws BadRequestException {
        Annotation annotation2 = annotation.copy(builder, evoBuilder, this);
        return annotation2;
    }


    /**
     * Get an annotation URI based on the id.
     * 
     * @param annotationId
     *            annotation id, random UUID will be used if null
     * @return the annotation URI
     */
    private URI getAnnotationUri(String annotationId) {
        if (annotationId == null) {
            annotationId = UUID.randomUUID().toString();
        }
        URI annotationUri = uri.resolve(".ro/annotations/" + annotationId);
        return annotationUri;
    }


    /**
     * Delete the RO index.
     * 
     */
    public void deleteIndexAttributes() {
    }


    /**
     * Update the RO index.
     */
    public void updateIndexAttributes() {
    }
    
    public Mode getMode(){
    	AccessMode accessMode = accessModeDao.findByResearchObject(getUri().toString());
    	if(accessMode == null || accessMode.getMode() == Mode.PRIVATE){
    		return Mode.PRIVATE;
    	} else {
    		return Mode.PUBLIC;
    	}
    	
    }


    /**
     * Create a new research object submitted in ZIP format.
     * 
     * @param builder
     *            model instance builder
     * @param researchObjectUri
     *            the new research object
     * @param zip
     *            the ZIP file
     * @param status
     *            the status of proceeded operation
     * @return HTTP response (created in case of success, 404 in case of error)
     * @throws IOException
     *             error creating the temporary filez
     * @throws BadRequestException
     *             the ZIP contains an invalid RO
     */
    public static ResearchObject create(Builder builder, URI researchObjectUri, MemoryZipFile zip,
            ROFromZipJobStatus status)
            throws IOException, BadRequestException {
        status.setProcessedResources(0);
        Dataset dataset = DatasetFactory.createMem();
        Builder inMemoryBuilder = new Builder(builder.getUser(), dataset, false);
        Model model = ModelFactory.createDefaultModel();
        try (InputStream manifest = zip.getManifestAsInputStream()) {
            if (manifest == null) {
                throw new BadRequestException("Manifest not found");
            }
            model.read(manifest, researchObjectUri.resolve(ResearchObject.MANIFEST_PATH).toString(), "RDF/XML");
            dataset.addNamedModel(researchObjectUri.resolve(ResearchObject.MANIFEST_PATH).toString(), model);
            //System.out.println("Manifest in Turtle");
            //model.write(System.out, "TURTLE");
        }
        ResearchObject inMemoryResearchObject = inMemoryBuilder.buildResearchObject(researchObjectUri);
        ResearchObject researchObject = create(builder, researchObjectUri);
        
        int submittedresources = 0;
        for (Resource resource : inMemoryResearchObject.getResources().values()) {
            if (resource.isSpecialResource()) {
                continue;
            } else {
                submittedresources++;
            }
        }
        for (Annotation annotation : inMemoryResearchObject.getAnnotations().values()) {
            try {
                if (inMemoryResearchObject.getAggregatedResources().containsKey(annotation.getBody().getUri())) {
                    AggregatedResource body = inMemoryResearchObject.getAggregatedResources().get(
                        annotation.getBody().getUri());
                    if (body.isSpecialResource()) {
                        continue;
                    }
                    submittedresources++;
                }
            } catch (Exception e) {
                LOGGER.error("Error when aggregating annotations", e);
            }
        }
        status.setSubmittedResources(submittedresources);
        LOGGER.debug("start copying resources: " + DateTime.now());
        for (Resource resource : inMemoryResearchObject.getResources().values()) {
        	
            if (resource.isSpecialResource()) {
                continue;
            }
            try {
                if (zip.containsEntry(resource.getPath())) {
                	if (resource.getPath().contains("bundle.zip")){
                		unpackAndAggregate(researchObject, zip, resource.getPath(),
                				RoBundle.MIME_TYPE);
                	}
                	else{
                		unpackAndAggregate(researchObject, zip, resource.getPath(),
                				MimeTypeUtil.getContentType(resource.getPath()));
                	}
                    LOGGER.debug("Aggregated an internal resource " + resource.getUri());
                } else {
                    researchObject.aggregate(resource.getUri());
                    LOGGER.debug("Aggregated an external resource " + resource.getUri());
                }
                if (status.getProcessedResources() < status.getSubmittedResources()) {
                    status.setProcessedResources(status.getProcessedResources() + 1);
                }
            } catch (Exception e) {
                LOGGER.error("Error when aggregating resources", e);
            }
        }
        LOGGER.debug("start copying annotations: " + DateTime.now());
        for (Annotation annotation : inMemoryResearchObject.getAnnotations().values()) {
            try {
            	LOGGER.debug("working now with annotation: " + annotation.getUri()+" and body: "+annotation.getBody().getUri());
                if (inMemoryResearchObject.getAggregatedResources().containsKey(annotation.getBody().getUri())) {
                    AggregatedResource body = inMemoryResearchObject.getAggregatedResources().get(
                        annotation.getBody().getUri());
                    if (body.isSpecialResource()) {
                        continue;
                    }
                    
                    if (!researchObject.getAggregatedResources().containsKey(researchObject.getUri().resolve(body.getPath()))){
                    	unpackAndAggregate(researchObject, zip, body.getPath(),
                    			RDFFormat.forFileName(body.getPath(), RDFFormat.RDFXML).getDefaultMIMEType());
                    	LOGGER.debug("Aggregated an internal annotation body " + body.getUri());
                    }
                }
                
                Annotation annotation2 = researchObject.annotate(annotation.getBody().getUri(), annotation.getAnnotated());
                LOGGER.debug("Aggregated an annotation with body " + annotation2.getBody().getUri());
                if (status.getProcessedResources() < status.getSubmittedResources()) {
                    status.setProcessedResources(status.getProcessedResources() + 1);
                }
            } catch (Exception e) {
                LOGGER.error("Error when aggregating annotations", e);
            }
        }
        
        LOGGER.debug("start copying folders: " + DateTime.now());
        for (Folder folder : inMemoryResearchObject.getFolders().values()) {
        	try {
        		URI fixedURI; //to aggregate a folder properly (in hierarchy it needs to end with "/", why is not like this?
        		if (!folder.getUri().toString().endsWith("/")) {
        			fixedURI=new URI(folder.getUri().toString().concat("/"));
        		}
        		else{
        			fixedURI=new URI(folder.getUri().toString());
        		}
        		LOGGER.debug("Aggregating folder " + fixedURI.toString());
				researchObject.aggregateFolder(fixedURI);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	//researchObject.aggregateFolder(folder.getUri());
        	//researchObject.aggregateFolder(researchObject.getUri().resolve(folder.getPath().replace(" ", "%20"))); //APPARENTLY NOT NECESSARY - FOLDER.GETURI IS ALREADY RESOLVED
        }
        
        /*
         * THIS LOOP WAS USED TEMPORARLY BECAUSE RESOURCEMAPS IN MYEXPERIMENT DONT HAVE
         * CORRECT ORE:AGGREGATES, I.E., ALL FOLDERS (EXCEPT FROM LEAF LEVEL)
         * ONLY INCLUDE ORE:AGGREGATE OF SUBFOLDERS NOT RESOURCES...
         * BUT WE SHOULD STICK TO THE CORRECT WAY BELOW
         */
        /*
        for (Entry<URI, Folder> entryFolder : researchObject.getFolders().entrySet()){
        	String queryString = String
        	                .format(
        	                	"PREFIX ore: <%s> PREFIX ro: <%s> SELECT ?resource WHERE { <%s> ore:aggregates ?resource . ?resource a ro:Resource . }",
        	                    ORE.NAMESPACE, RO.NAMESPACE, entryFolder.getKey().toString());	
        	Query query = QueryFactory.create(queryString);
        	LOGGER.debug("entry: "+entryFolder.getKey().toString()+"query: "+query+" model "+model);
        	QueryExecution qe = QueryExecutionFactory.create(query, model);
        	try {
        		ResultSet results = qe.execSelect();
        	    while (results.hasNext()) {
        	    	QuerySolution solution = results.next();
        			RDFNode f = solution.get("resource");
        			URI resourceUri = researchObject.getUri().resolve(f.asResource().getURI());
        			AggregatedResource resource = researchObject.getResources().get(resourceUri); 
                    if (resource == null) {
                    	resource = researchObject.getFolders().get(resourceUri);
                    	if (resource == null) {
                    		LOGGER.warn("Resource for entry not found: " + resourceUri);
                            continue;
                        }
                    }
                    entryFolder.getValue().createFolderEntry(resource);
                    LOGGER.debug("Created an entry for " + resource.getUri() + " in " + entryFolder.getValue().getUri());
                }
        	} finally {
        		qe.close();
        	}
        }
        */
        /* THIS IS THE CORRECT WAY OF ADDING FOLDER ENTRIES (INSTEAD OF LAST FOR LOOP)
        */
        LOGGER.debug("start copying folder entries: " + DateTime.now());
        for (Entry<URI, Folder> entryFolder : researchObject.getFolders().entrySet()){
        	try {
        		URI fixedURI; //but to get folder from original RO, it should not end with "/" 
        		if (entryFolder.getKey().toString().endsWith("/")) {
        			fixedURI=new URI(entryFolder.getKey().toString().substring(0, entryFolder.getKey().toString().length()-1));
        		}
        		else{
        			fixedURI=new URI(entryFolder.getKey().toString());
        		}
        		//Folder folder = inMemoryResearchObject.getFolders().get(entryFolder.getKey());
        		Folder folder = inMemoryResearchObject.getFolders().get(fixedURI);
            	InputStream is = zip.getFolderResourceMap(folder.getResourceMap().getPath());
            	Model model2 = ModelFactory.createDefaultModel();
            	String base = folder.getResourceMap().getPath().replaceAll(" ","%20");
            	if (base.lastIndexOf("/")>0)
            		base=base.substring(0, base.lastIndexOf("/")+1);
            	model2.read(is, researchObjectUri.resolve(base).toString(), "RDF/XML");
                //model2.write(System.out, "TURTLE"); //print resource map in Turtle
            	
            	String queryString = String
    	                .format(
    	                	"PREFIX ore: <%s> PREFIX ro: <%s> SELECT ?resource WHERE { <%s> ore:aggregates ?resource . }",
    	                    ORE.NAMESPACE, RO.NAMESPACE, entryFolder.getKey().toString());	
            	Query query = QueryFactory.create(queryString);
            	LOGGER.debug("entry: "+entryFolder.getKey().toString() +" entry-fixed: "+fixedURI.toString() +" base: "+base+" query: "+query+" model "+model2);
            	QueryExecution qe = QueryExecutionFactory.create(query, model2);
            	try {
            		ResultSet results = qe.execSelect();
            	    while (results.hasNext()) {
            	    	QuerySolution solution = results.next();
            			RDFNode f = solution.get("resource");
            			URI resourceUri = researchObject.getUri().resolve(f.asResource().getURI());
            			LOGGER.debug("resourceURI entry: "+resourceUri.toString());
            			AggregatedResource resource = researchObject.getResources().get(resourceUri);//try resource 
            			if (resource == null) { //try folder
                        	URI fixedresourceUri; //but in the new RO the folder ends with "/"
                    		if (!resourceUri.toString().endsWith("/")) {
                    			fixedresourceUri=new URI(resourceUri.toString().concat("/"));
                    		}
                    		else{
                    			fixedresourceUri=new URI(resourceUri.toString());
                    		}
                    		LOGGER.debug("resourceURI fixed (entry for folder): "+fixedresourceUri.toString());
                        	resource = researchObject.getFolders().get(fixedresourceUri);
                        	if (resource == null) {
                        		LOGGER.warn("Resource for entry not found: " + resourceUri);
                                continue;
                            }
                        }
                        entryFolder.getValue().createFolderEntry(resource);
                        LOGGER.debug("Created an entry for " + resource.getUri() + " in " + entryFolder.getValue().getUri());
            	    }
            	}finally {
            		qe.close();
            	}
        		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
        
        LOGGER.debug("finished create from zip: " + DateTime.now());
        dataset.close();
        return researchObject;
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
    public static ResearchObject create(URI uri, ResearchObject researchObject, Builder builder,
            EvoType evoType) {
        ResearchObjectIdDAO idDAO = new ResearchObjectIdDAO();
        uri = idDAO.assignId(new ResearchObjectId(uri)).getId();
        if (ResearchObject.get(builder, uri) != null) {
            throw new ConflictException("Research Object already exists: " + uri);
        }
        ResearchObject copyRO = builder.buildResearchObject(uri,
            researchObject.getCreator(), researchObject.getCreated());
        copyRO.setCopyDateTime(DateTime.now());
        copyRO.setCopyAuthor(builder.getUser());
        copyRO.setCopyOf(researchObject);
       // copyRO.copy(researchObject.getManifest(), evoBuilder);
        copyRO.save(evoType);
        EvoBuilder evoBuilder = null;
        // copy the ro:Resources
        for (org.rohub.rodl.model.RO.Resource resource : researchObject.getResources().values()) {
            try {
                copyRO.copy(resource, evoBuilder);
            } catch (BadRequestException e) {
                LOGGER.warn("Failed to copy the resource", e);
            }
        }
        //copy the annotations
        for (Annotation annotation : researchObject.getAnnotations().values()) {
            try {
                copyRO.copy(annotation, evoBuilder);
            } catch (BadRequestException e) {
                LOGGER.warn("Failed to copy the annotation", e);
            }
        }
        //sort the folders topologically
        List<Folder> sorted = new ArrayList<>();
        for (Folder folder : researchObject.getFolders().values()) {
            if (!sorted.contains(folder)) {
                sorted.addAll(visit(folder, sorted));
            }
        }
        //copy the folders
        for (Folder folder : sorted) {
            copyRO.copy(folder, evoBuilder);
        }
        return copyRO;
    }
    
    public static ResearchObject createFork(URI uri, ResearchObject sourceRO, Builder builder,
            EvoType evoType) throws BadRequestException, URISyntaxException {
    	 ResearchObjectIdDAO idDAO = new ResearchObjectIdDAO();
         uri = idDAO.assignId(new ResearchObjectId(uri)).getId();
         if (ResearchObject.get(builder, uri) != null) {
             throw new ConflictException("Research Object already exists: " + uri);
         }
         ResearchObject copyRO = builder.buildResearchObject(uri,
             sourceRO.getCreator(), sourceRO.getCreated());
         copyRO.setCreated(DateTime.now());
         copyRO.setCreator(builder.getUser());
         copyRO.setCopyOf(sourceRO);
         copyRO.save(EvoType.FORK);
         EvoBuilder evoBuilder = null;
         // copy the ro:Resources
         LOGGER.debug("start copying resources: " + DateTime.now());
         for (org.rohub.rodl.model.RO.Resource resource : sourceRO.getResources().values()) {
             try {
            	 LOGGER.debug("Copy resource: " + resource.getName());
                 copyRO.copy(resource, evoBuilder);
             } catch (BadRequestException e) {
                 LOGGER.warn("Failed to copy the resource", e);
             }
         }
         //copy the annotations
         LOGGER.debug("start copying annotations: " + DateTime.now());
         for (Annotation annotation : sourceRO.getAnnotations().values()) {
             try {
            	 LOGGER.debug("Copy annotation: " + annotation.getName()+" body: "+annotation.getBody());            	 
            	 if(annotation.containsTripple(sourceRO.getUri().toString(), RDF.type.toString(), RO.ResearchObject.getURI()) ||
            		annotation.containsTripple(sourceRO.getUri().toString(), RDF.type.toString(), ROEVO.LiveRO.getURI()) ||
            		annotation.containsTripple(sourceRO.getUri().toString(), RDF.type.toString(), ROEVO.SnapshotRO.getURI()) ||
            		annotation.containsTripple(null, DCTerms.isReferencedBy.toString(), null) || 
            		annotation.containsTripple(null, DCTerms.references.toString(), null)) {
            		 // do not copy above annotations
            		 continue;
            	 }
            	 LOGGER.debug("not filtered: " + annotation.getName());
                 copyRO.copy(annotation, evoBuilder);
                 
             } catch (BadRequestException e) {
                 LOGGER.warn("Failed to copy the annotation", e);
             }
         }
         //sort the folders topologically
         LOGGER.debug("start sorting folders: " + DateTime.now());
         List<Folder> sorted = new ArrayList<>();
         for (Folder folder : sourceRO.getFolders().values()) {
             if (!sorted.contains(folder)) {
                 sorted.addAll(visit(folder, sorted));
             }
         }
         //copy the folders
         LOGGER.debug("start copying folders: " + DateTime.now());
         for (Folder folder : sorted) {
             copyRO.copy(folder, evoBuilder);
         }
         LOGGER.debug("start creating special annotations: " + DateTime.now());
         addAnnotation(copyRO, DCTerms.references, sourceRO);
         addAnnotation(sourceRO, DCTerms.isReferencedBy, copyRO);
 		
         copyRO.postEvent(new ROForkAfterCreateEvent(copyRO));
         LOGGER.debug("finished create fork: " + DateTime.now());
         return copyRO;
    }
    
    
    protected static void addAnnotation(ResearchObject subject, Property p, ResearchObject object) throws BadRequestException, URISyntaxException{
    	Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

    	String path = ".ro/annotations/" + 
				UUID.randomUUID().toString() + "." + RDFFormat.RDFXML.getDefaultFileExtension();
    	
		com.hp.hpl.jena.rdf.model.Resource sub = model.createResource(subject.getUri().toString());
		com.hp.hpl.jena.rdf.model.Resource obj = model.createResource(object.getUri().toString());
		
		model.add(sub, p, obj);	

		StringWriter out = new StringWriter();
		model.write(out, RDFFormat.RDFXML.getName(), null);
		
		ByteArrayInputStream data = new ByteArrayInputStream(out.toString().getBytes());

		Builder oldBuilder = subject.getBuilder();
		Builder newBuilder = new Builder(ServiceUserMetadata.RODL, 
										oldBuilder.getDataset(), 
										oldBuilder.isUseTransactions(),
										oldBuilder.getDigitalLibrary());
		subject.setBuilder(newBuilder);
		
		subject.aggregate(path, data,
				RDFFormat.RDFXML.getDefaultMIMEType());

		// crate annotation body URI
		// annotation body uri should be relative to the RO
		UriBuilder uriBuilder = UriBuilder.fromUri(subject.getUri());
		URI resourceUri = uriBuilder.path(path).build();
		
		subject.annotate(resourceUri, subject);
		subject.setBuilder(oldBuilder);

    }

    /**
     * Unpack a resource from the ZIP archive and aggregate to the research object.
     * 
     * @param researchObject
     *            research object to which to aggregate
     * @param zip
     *            ZIP archive
     * @param path
     *            resource path
     * @param mimeType
     *            resource MIME type
     * @throws IOException
     *             when there is an error handling the temporary file or the zip archive
     * @throws FileNotFoundException
     *             when the temporary file cannot be read
     * @throws BadRequestException
     *             when the resource should be an annotation body but is not an RDF file
     */
    private static void unpackAndAggregate(ResearchObject researchObject, MemoryZipFile zip, String path,
            String mimeType)
            throws IOException, FileNotFoundException, BadRequestException {
        UUID uuid = UUID.randomUUID();
        File tmpFile = File.createTempFile("tmp_resource", uuid.toString());
        try (InputStream is = zip.getEntryAsStream(path)) {
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(is, fileOutputStream);
            researchObject.aggregate(path, new FileInputStream(tmpFile), mimeType);
        } finally {
            tmpFile.delete();
        }
    }


    /**
     * Get the manifest URI.
     * 
     * @return manifest URI
     */
    public URI getManifestUri() {
        return uri != null ? uri.resolve(MANIFEST_PATH) : null;
    }


    /**
     * Get the roevo annotation body URI.
     * 
     * @return roevo annotation body URI
     */
    public URI getFixedEvolutionAnnotationBodyUri() {
        return getUri().resolve(ROEVO_PATH);
    }


    /**
     * Get aggregated ro:Resources, excluding ro:Folders, loaded lazily.
     * 
     * @return aggregated resources mapped by their URI
     */
    public Map<URI, Resource> getResources() {
        if (resources == null) {
            this.resources = getManifest().extractResources();
        }
        return resources;
    }


    /**
     * Get aggregated ro:Folders, loaded lazily.
     * 
     * @return aggregated folders mapped by their URI
     */
    public Map<URI, Folder> getFolders() {
        if (folders == null) {
            this.folders = getManifest().extractFolders();
        }
        return folders;
    }


    /**
     * Get folder entries of all folders.
     * 
     * @return folder entries mapped by the URIs.
     */
    public Map<URI, FolderEntry> getFolderEntries() {
        if (folderEntries == null) {
            folderEntries = new HashMap<>();
            for (Folder folder : getFolders().values()) {
                folderEntries.putAll(folder.getFolderEntries());
            }
        }
        return folderEntries;
    }


    /**
     * Get folder entries grouped by the URI of the resource they proxy. Loaded lazily.
     * 
     * @return multimap of folder entries
     */
    public Multimap<URI, FolderEntry> getFolderEntriesByResourceUri() {
        if (folderEntriesByResourceUri == null) {
            folderEntriesByResourceUri = HashMultimap.<URI, FolderEntry> create();
            for (FolderEntry entry : getFolderEntries().values()) {
                if (entry.getProxyFor() != null) {
                    folderEntriesByResourceUri.put(entry.getProxyFor().getUri(), entry);
                } else {
                    LOGGER.warn("Folder entry " + entry + " has no proxy for");
                }
            }
        }
        return folderEntriesByResourceUri;
    }


    /**
     * Get proxies for aggregated resources, loaded lazily.
     * 
     * @return proxies mapped by their URI
     */
    @Override
    public Map<URI, Proxy> getProxies() {
        if (proxies == null) {
            this.proxies = new HashMap<>();
            for (AggregatedResource aggregatedResource : this.getAggregatedResources().values()) {
                Proxy proxy = aggregatedResource.getProxy();
                if (proxy != null) {
                    this.proxies.put(proxy.getUri(), proxy);
                }
            }
        }
        return proxies;
    }


    /**
     * Get aggregated annotations, loaded lazily.
     * 
     * @return aggregated annotations mapped by their URI
     */
    public Map<URI, Annotation> getAnnotations() {
        if (annotations == null) {
            this.annotations = getManifest().extractAnnotations();
        }
        return annotations;
    }


    /**
     * Get aggregated annotations, mapped by the annotated resources, loaded lazily.
     * 
     * @return aggregated annotations mapped by annotated resources URIs
     */
    public Multimap<URI, Annotation> getAnnotationsByTarget() {
        if (annotationsByTargetUri == null) {
            this.annotationsByTargetUri = HashMultimap.<URI, Annotation> create();
            for (Annotation ann : getAnnotations().values()) {
                for (Thing target : ann.getAnnotated()) {
                    this.annotationsByTargetUri.put(target.getUri(), ann);
                }
            }
        }
        return annotationsByTargetUri;
    }


    /**
     * Get aggregated annotations, mapped by the bodies, loaded lazily.
     * 
     * @return aggregated annotations mapped by body URIs
     */
    public Multimap<URI, Annotation> getAnnotationsByBodyUri() {
        if (annotationsByBodyUri == null) {
            this.annotationsByBodyUri = HashMultimap.<URI, Annotation> create();
            for (Annotation ann : getAnnotations().values()) {
                this.annotationsByBodyUri.put(ann.getBody().getUri(), ann);
            }
        }
        return annotationsByBodyUri;
    }


    /**
     * Get the aggregated resource. Load the metadata first, if necessary.
     * 
     * @return a map of aggregated resource by their URI
     */
    @Override
    public Map<URI, AggregatedResource> getAggregatedResources() {
        if (aggregatedResources == null) {
            this.aggregatedResources = getManifest().extractAggregatedResources(getResources(), getFolders(),
                getAnnotations());
        }
        return aggregatedResources;
    }


    @Override
    public ResourceMap getResourceMap() {
        return getManifest();
    }


    /**
     * Get manifest and folder resource maps, loading the lazily.
     * 
     * @return folder resource maps mapped by their URIs
     */
    public Map<URI, ResourceMap> getResourceMaps() {
        if (resourceMaps == null) {
            this.resourceMaps = new HashMap<>();
            this.resourceMaps.put(getManifest().getUri(), getManifest());
            for (Folder folder : getFolders().values()) {
                resourceMaps.put(folder.getResourceMap().getUri(), folder.getResourceMap());
            }
        }
        return resourceMaps;
    }


    @Override
    public DateTime getCreated() {
        if (created == null) {
            this.created = getManifest().extractCreated(this);
        }
        return super.getCreated();
    }


    @Override
    public UserMetadata getCreator() {
        if (creator == null) {
            this.creator = getManifest().extractCreator(this);
        }
        return super.getCreator();
    }


    /**
     * Is there already a resource in this RO with that URI.
     * 
     * @param uri
     *            the URI
     * @return true if there is an aggregated resource / proxy / folder resource map / manifest / folder entry with that
     *         URI
     */
    public boolean isUriUsed(URI uri) {
        return getAggregatedResources().containsKey(uri) || getProxies().containsKey(uri)
                || getFolderEntries().containsKey(uri) || getResourceMaps().containsKey(uri);
    }


    public InputStream getAsZipArchive() {
        return builder.getDigitalLibrary().getZippedResearchObject(uri);
    }


    /**
     * Get all research objects. If the user is set whose role is not public, only the user's research objects are
     * looked for.
     * 
     * @param builder
     *            builder that defines the dataset and the user
     * @param userMetadata
     *            the user to filter the results
     * @return a set of research objects
     */
    public static Set<ResearchObject> getAll(Builder builder, UserMetadata userMetadata) {
        boolean wasStarted = builder.beginTransaction(ReadWrite.READ);
        try {
            Set<ResearchObject> ros = new HashSet<>();
            String queryString;
            if (userMetadata == null || userMetadata.getRole() == Role.PUBLIC) {
                queryString = String.format(
                    "PREFIX ro: <%s> SELECT ?ro WHERE { GRAPH ?g { ?ro a ro:ResearchObject . } }", RO.NAMESPACE);
            } else {
                queryString = String
                        .format(
                            "PREFIX ro: <%s> PREFIX dcterms: <%s> SELECT ?ro WHERE { GRAPH ?g { ?ro a ro:ResearchObject ; dcterms:creator <%s> . } }",
                            RO.NAMESPACE, DCTerms.NS, userMetadata.getUri());
            }
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, builder.getDataset());
            try {
                ResultSet results = qe.execSelect();
                while (results.hasNext()) {
                    QuerySolution solution = results.next();
                    RDFNode r = solution.get("ro");
                    URI rUri = URI.create(r.asResource().getURI());
                    ros.add(builder.buildResearchObject(rUri));
                }
            } finally {
                qe.close();
            }
            return ros;
        } finally {
            builder.endTransaction(wasStarted);
        }
    }


    public EvoInfo getEvoInfo() {
        return getLiveEvoInfo();
    }


    @Override
    public Map<URI, ResearchObjectComponentSerializable> getSerializables() {
        HashMap<URI, ResearchObjectComponentSerializable> result = new HashMap<>();

        for (URI uri : getAggregatedResources().keySet()) {
            if (getAggregatedResources().get(uri).isInternal()) {
                result.put(uri, getAggregatedResources().get(uri));
            }
        }
        for (URI uri : getResourceMaps().keySet()) {
            if (getResourceMaps().get(uri) != null && getResourceMaps().get(uri).isInternal()) {
                result.put(uri, getResourceMaps().get(uri));
            }
        }
        return result;
    }


    /**
     * Return the URI of the RO bundle if exists.
     * 
     * @return the URI of this RO's bundle or null if doesn't exist
     */
    public URI getBundleUri() {
        if (bundleUri == null) {
            bundleUri = getManifest().extractAlternativeFormat(RoBundle.MIME_TYPE);
        }
        return bundleUri;
    }


    /**
     * Save the URI of the RO bundle representing this RO.
     * 
     * @param bundleUri
     *            the URI of this RO's bundle
     */
    public void setBundleUri(URI bundleUri) {
        this.bundleUri = bundleUri;
        getManifest().saveAlternativeFormat(bundleUri, RoBundle.MIME_TYPE);
    }


    /**
     * Return the serialization as RO Bundle if available.
     * 
     * @return the serialized RO bundle or null if not available
     */
    public InputStream getBundle() {
        URI bundleUri2 = getBundleUri();
        if (bundleUri2 == null) {
            return null;
        }
        // The bundle may be stored inside the parent RO. The path may then start with ../[parentRO]/.
        Path bundlePath = Paths.get(uri.getPath()).relativize(Paths.get(bundleUri2.getPath()));
        return builder.getDigitalLibrary().getFileContents(uri, bundlePath.toString());
    }


    /**
     * Return the URI of the RO bundle if exists.
     * 
     * @return the URI of this RO's bundle or null if doesn't exist
     */
    public Collection<URI> getAggregatingROUris() {
        if (aggregatingROUris == null) {
            aggregatingROUris = getManifest().extractAggregatingROUris();
        }
        return aggregatingROUris;
    }
    
    /**
     * Sort folders topologically based on subfolders.
     * 
     * @param folder
     *            folder to visit
     * @param sorted
     *            all already sorted folders
     * @return this folder and all not visited subfolders, sorted topologically
     */
    protected static List<Folder> visit(Folder folder, List<Folder> sorted) {
        List<Folder> list = new ArrayList<>();
        for (FolderEntry entry : folder.getFolderEntries().values()) {
            if (entry.getProxyFor() instanceof Folder && !sorted.contains(entry.getProxyFor())) {
                list.addAll(visit((Folder) entry.getProxyFor(), sorted));
            }
        }
        list.add(folder);
        return list;
    }
    
    public int cntArchives(){
    	String query = "PREFIX roevo: <http://purl.org/wf4ever/roevo#> " +
        	    "	select (count(?s) as ?cnt) where {  " +
        	    "	?s roevo:isArchiveOf ?ro  " +
        		"}";
    	return count(query);
    }
    
    public int cntSnapshots(){
    	String query = "PREFIX roevo: <http://purl.org/wf4ever/roevo#> " +
        		"	select (count(?s) as ?cnt) where { " +
        		"	?s roevo:isSnapshotOf ?ro " +
        		"}";
    	return count(query);
    }
    
    public int cntForks(){
    	String query = "PREFIX prov:<http://www.w3.org/ns/prov#> " +
        	    "	select (count(?s) as ?cnt) where {  " +
        	    "	?s prov:wasDerivedFrom ?ro  " +
        		"}";
    	return count(query);
    }
    
    public boolean hasDoi(){
    	String query = 
    			"select (count(?doi) as ?cnt) where { " +
    			"?ro <http://purl.org/ontology/bibo/doi> ?doi . " +
    			"?ro <http://purl.org/dc/terms/identifier> ?doi " +
    			"}";
    	return (count(query) > 0 ? true : false);
    }
    
	protected int count(String cntQuery){
		int value = 0;
		dataset.begin(ReadWrite.READ);
		try {
	    	ParameterizedSparqlString paramSparql = new ParameterizedSparqlString();
			paramSparql.setCommandText(cntQuery);
			paramSparql.setParam("ro", this.dataset.getDefaultModel().createResource(this.getUri().toString()));
	
			Query query = QueryFactory.create(paramSparql.asQuery()) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
	
	    	try{
	    		ResultSet rs = qexec.execSelect();
	    		if(rs.hasNext()){
	    			String cnt = rs.next().getLiteral("cnt").getString();
	    			value = Integer.parseInt(cnt);
	    		}
	    	} finally {
	    		qexec.close();
	    	}
	    } finally {
			dataset.end();
	    }
		
		return value;
	}

	protected String locateAnnotationBodyURI(String subject, String predicate, String object){
		String graphId = null;
		dataset.begin(ReadWrite.READ);
		try {
	    	ParameterizedSparqlString paramSparql = new ParameterizedSparqlString();
			paramSparql.setCommandText(
						"select ?g where { " + 
						"	graph ?g { " +
						"		?s ?p ?o" +
						"	} " +
						"}");
			
			if(subject != null)
				paramSparql.setParam("s", this.dataset.getDefaultModel().createResource(subject));
			if(predicate != null)
				paramSparql.setParam("p", this.dataset.getDefaultModel().createResource(predicate));
			if(object != null)
				paramSparql.setParam("o", this.dataset.getDefaultModel().createResource(object));
	
			Query query = QueryFactory.create(paramSparql.asQuery()) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
	
	    	try{
	    		ResultSet rs = qexec.execSelect();
	    		if(rs.hasNext())
	    			graphId = rs.next().getResource("g").getURI();
	    	} finally {
	    		qexec.close();
	    	}
	    } finally {
			dataset.end();
	    }
		
		return graphId;
	}
	
	protected String getDerivedFromRo(){
		String source = null;
		dataset.begin(ReadWrite.READ);
		try {
	    	ParameterizedSparqlString paramSparql = new ParameterizedSparqlString();
			paramSparql.setCommandText(
						"PREFIX prov:<http://www.w3.org/ns/prov#> " + 
						"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
						"select * where { " +
						"	?ro rdf:type <http://purl.org/wf4ever/roevo#ForkedRO> . " +
						"	?ro prov:wasDerivedFrom ?src " +
						"}"
						);
			
			paramSparql.setParam("ro", this.dataset.getDefaultModel().createResource(this.uri.toString()));

			Query query = QueryFactory.create(paramSparql.asQuery()) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
	
	    	try{
	    		ResultSet rs = qexec.execSelect();
	    		if(rs.hasNext())
	    			source = rs.next().getResource("src").getURI();
	    	} finally {
	    		qexec.close();
	    	}
	    } finally {
			dataset.end();
	    }
	
		return source;
	}
	
	/**
	 * 
	 * @return derived from RO
	 */
	public ResearchObject cleanForkRelationships(){
		ResearchObject derivedFromRO = null;
		String derivedFromROuri = getDerivedFromRo();
		if(derivedFromROuri == null)
    		return null;
    	
    	try {
    		derivedFromRO = ResearchObject.get(builder, new URI(derivedFromROuri));
    		if(derivedFromRO == null)
    			return null;
    			
			String annBodyURI = derivedFromRO.locateAnnotationBodyURI(derivedFromROuri, DCTerms.isReferencedBy.toString(), this.uri.toString());
			if(annBodyURI == null)
				return null;
			
			URI bodyURI = new URI(annBodyURI);
			AggregatedResource aggRes = derivedFromRO.getAggregatedResources().get(bodyURI);
			Annotation ann = derivedFromRO.getAnnotationsByBodyUri().get(bodyURI).iterator().next();
			
			aggRes.delete();
			ann.delete();
			
    	} catch (Exception e){
    		LOGGER.debug(e.getMessage(), e);
    		return null;
    	}
    	
		return derivedFromRO;
	}
	
	public EvoType checkEvoType(){
		HashSet<String> roTypes = new HashSet<String>();
		
		dataset.begin(ReadWrite.READ);
		try {
			String stringQuery = "select ?type where { ?ro ?p ?type }";
	    	ParameterizedSparqlString paramSparql = new ParameterizedSparqlString();
			paramSparql.setCommandText(stringQuery);
			paramSparql.setParam("ro", this.dataset.getDefaultModel().createResource(this.getUri().toString()));
			paramSparql.setParam("p", RDF.type);
			Query query = QueryFactory.create(paramSparql.asQuery()) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
			
	    	try{
	    		
	    		ResultSet rs = qexec.execSelect();
	    		while(rs.hasNext()){
	    			QuerySolution solution = rs.next();
	    			roTypes.add(solution.get("type").toString());
	    		}
	    	} finally {
	    		qexec.close();
	    	}
	    } finally {
			dataset.end();
	    }
		
		if(roTypes.contains(ROEVO.ArchivedRO.toString())){
			return EvoType.ARCHIVE;
		} else if(roTypes.contains(ROEVO.SnapshotRO.toString())){
			return EvoType.SNAPSHOT;
		} else if(roTypes.contains(ROEVO.ForkedRO.toString())){
			return EvoType.FORK;
		} else if(roTypes.contains(ROEVO.LiveRO.toString())){
			return EvoType.LIVE;
		}
		
		return null;
	}
	
}
