package org.rohub.rodl.zip;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.Operation;
import org.rohub.rodl.job.OperationFailedException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.Folder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.model.RO.Resource;
import org.rohub.rodl.utils.MimeTypeUtil;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * Operation which stores a research object given in a zip format from outside.
 * 
 * @author pejot
 * 
 */
public class CreateROFromGivenZipOperation implements Operation {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(CreateROFromGivenZipOperation.class);
    /** resource builder. */
    protected Builder builder;
    /** zip input stream. */
    protected File zipFile;
    /** request uri info. */
    protected UriInfo uriInfo;


    /**
     * Constructor.
     * 
     * @param builder
     *            model instance builder
     * @param zipFile
     *            processed zip file
     * @param uriInfo
     *            reqest uri info
     */
    public CreateROFromGivenZipOperation(Builder builder, File zipFile, UriInfo uriInfo) {
        this.builder = builder;
        this.zipFile = zipFile;
        this.uriInfo = uriInfo;
    }


    @Override
    public void execute(JobStatus status)
            throws OperationFailedException {
        if (zipFile == null) {
            throw new OperationFailedException("Given zip is empty or it's null");
        }
        ROFromZipJobStatus roFromZipJobStatus = (ROFromZipJobStatus) status;
        boolean started = !HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive();
        if (started) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        }
        try {
            URI roUri = uriInfo.getBaseUri().resolve("ROs/").resolve(status.getTarget().toString());
            ResearchObject created = ResearchObject.create(builder, roUri);
            try {
                Map<String, Folder> createdFolders = new HashMap<>();
                @SuppressWarnings("resource")
                ZipFile zip = new ZipFile(zipFile);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                int submittedResources = 0;
                while (entries.hasMoreElements()) {
                    entries.nextElement();
                    submittedResources++;
                }
                roFromZipJobStatus.setSubmittedResources(submittedResources);
                entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    addEntry(created, entry.getName(), zip.getInputStream(entry), createdFolders);
                    zip.getInputStream(entry).close();
                    if (roFromZipJobStatus.getProcessedResources() < roFromZipJobStatus.getSubmittedResources()) {
                        roFromZipJobStatus.setProcessedResources(roFromZipJobStatus.getProcessedResources() + 1);
                    }
                }
                for (Entry<URI, Folder> entryFolder : created.getFolders().entrySet()){
                	Path path = Paths.get(entryFolder.getValue().getPath());
                	LOGGER.debug("folder to check: "+path);
					try {
						if (path.getParent()!=null){
							String parent=path.getParent().toString();
							if (!parent.endsWith("/")){
								parent=parent.concat("/");
							}
							URI parentURI;
							parentURI = new URI(parent);
							Folder f = created.getFolders().get(created.getUri().resolve(parentURI));
							if (f!=null){
								try{
									f.createFolderEntry(entryFolder.getValue());
									LOGGER.debug("entry added in: "+f.getName()+" for folder: "+entryFolder.getValue().getName());
								} catch (Exception e) {
									LOGGER.warn("could not make entry in: "+f.getName()+" for folder: "+entryFolder.getValue().getName());
								}
								
							}
						}
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            } catch (IOException | BadRequestException e) {
                throw new OperationFailedException("Can't preapre a ro from given zip", e);
            }
            status.setTarget(created.getUri());
        } finally {
            builder.getEventBusModule().commit();
            if (started) {
                HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            }
            zipFile.delete();
        }
    }


    /**
     * Rewrite entry from zip to ro.
     * 
     * @param ro
     *            Research Object
     * @param name
     *            entryName
     * @param inputStream
     *            resource Input Stream
     * @param createdFolders
     *            already created folders
     * @throws BadRequestException .
     */
    protected void addEntry(ResearchObject ro, String name, InputStream inputStream, Map<String, Folder> createdFolders)
            throws BadRequestException {
        //TODO can we make it more general?
        Path path = Paths.get(name);
        if (name.endsWith("/") || path.getFileName().toString().startsWith(".")) {
        	if (name.endsWith("/")){ //IS A FOLDER
        		try {
        			LOGGER.debug("adding folder: "+ro.getUri().resolve(name.replace(" ", "%20")));
        			Folder f = ro.aggregateFolder(ro.getUri().resolve(name.replace(" ", "%20")));
        			LOGGER.debug("folder key: "+name.substring(0, name.length()-1)+" folder value: "+ro.getUri().resolve(name.replace(" ", "%20")));
        			createdFolders.put(name.substring(0, name.length()-1), f);
        		} catch (ConflictException e){
            		LOGGER.warn("Folder existed: " + name);
            	}
        	}
        	else{
        		LOGGER.debug("Skipping " + name + ".\n");
        	}
        } else {
        	boolean resourceExisted = false;
            LOGGER.debug("Adding " + name + "... "+" with path: "+path);
            String contentType = MimeTypeUtil.getContentType(name);
            Resource resource = null;
            if(name.endsWith(".url")){
            	try {
	            	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
	            	String uri = reader.readLine();
	            	reader.close();
	            	LOGGER.debug("external resource url: " + uri);
	            	URI resUri = URI.create(uri);
	            	try {
	            		resource = ro.aggregate(resUri);
	            		
	            		try {
	            			String fileName = path.getFileName().toString();
	            			String title = fileName.substring(0, fileName.length() - 4);
		            		String annotationDoc = crateTitleAnnotationDoc(
					            										resUri.toString(), 
					            										title,
					            										RDFFormat.RDFXML);
		    				ByteArrayInputStream data = new ByteArrayInputStream(annotationDoc.toString().getBytes());
		    				String annPath = ".ro/" + UUID.randomUUID().toString() + "." + RDFFormat.RDFXML.getDefaultFileExtension();
		    				ro.aggregate(annPath, 
		    							data,
		    							RDFFormat.RDFXML.getDefaultMIMEType());
		    				// crate annotation body URI
		    				// annotation body uri should be relative to the RO
		    				UriBuilder uriBuilder = UriBuilder.fromUri(ro.getUri());
		    				URI annotationBodyURI = uriBuilder.path(annPath).build();
		    				
		    				ro.annotate(annotationBodyURI, resource);
	            		} catch (Exception e){
	            			LOGGER.warn("Unable to add title annotatation to external resource. " + e.getMessage());
	            		}
	            	} catch (ConflictException e){
	            		resource = (Resource) ro.getAggregatedResources().get(resUri);
	            		LOGGER.warn("Reusing existing external resource: " + resUri);
	            		if(resource == null){
	            			throw new ConflictException("Resource exists but can not find it " + resUri);
	            		}
	            	}
            	} catch (IOException e){
            		throw new BadRequestException(e.getMessage());
            	}
            } else {
            	try {
            		resource = ro.aggregate(name, inputStream, contentType);
            	} catch (ConflictException e){
            		LOGGER.warn("Resource existed: " + name);
            		resourceExisted=true;
            	}
            }
            
            boolean parentExisted = false;
            while (path.getParent() != null && !parentExisted &&!resourceExisted) {
            	LOGGER.debug("parent: " + path.getParent().toString());
                if (!createdFolders.containsKey(path.getParent().toString())) {
                    String folderName = path.getParent().toString();
                    LOGGER.debug(folderName);
                    if (!folderName.endsWith("/")) {
                        folderName += "/";
                    }
                    //ro.getUri().resolve(path.getParent().toString() + "/")
                    LOGGER.debug("folder to add: "+UriBuilder.fromUri(ro.getUri()).path(folderName).build().toString());
                    Folder f = ro.aggregateFolder(UriBuilder.fromUri(ro.getUri()).path(folderName).build());
                    LOGGER.debug("folder key: "+path.getParent().toString()+" folder value: "+f);
                    createdFolders.put(path.getParent().toString(), f);
                } else {
                    parentExisted = true;
                }
                Resource current = createdFolders.containsKey(path.toString()) ? createdFolders.get(path.toString())
                        : resource;
                createdFolders.get(path.getParent().toString()).createFolderEntry(current);
                path = path.getParent();
            }
            LOGGER.debug("done.\n");
        }
    }
    
    protected String crateTitleAnnotationDoc(String resourceUrl, String title, RDFFormat format) {

		OntModel model = ModelFactory.createOntologyModel();

		Literal literal = model.createLiteral(title);
		Property titleProperty = model.createProperty(DCTerms.title.toString());

		com.hp.hpl.jena.rdf.model.Resource resource = model.createResource(resourceUrl);

		model.add(resource, titleProperty, literal);

		StringWriter out = new StringWriter();
		model.write(out, format.getName(), null);

		return out.toString();

	}
}
