package org.rohub.rodl.zip;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.job.JobStatus;
import org.rohub.rodl.job.OperationFailedException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.Folder;
import org.rohub.rodl.model.RO.ResearchObject;

public class CreateResourceFromGivenZipOperation extends CreateROFromGivenZipOperation {

	private static final Logger LOGGER = Logger.getLogger(CreateResourceFromGivenZipOperation.class);
	
	public CreateResourceFromGivenZipOperation(Builder builder, File zipFile, UriInfo uriInfo) {
		super(builder, zipFile, uriInfo);
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
        	
            URI roUri = status.getTarget();
            ResearchObject ro = ResearchObject.get(builder, roUri);
            if(ro == null)
            	throw new OperationFailedException("Requested RO " + roUri + " not found");
            
            try {
                Map<String, Folder> createdFolders = new HashMap<>();
                String dst = status.getProperty("destination");
                String destination = "";
                Folder dstFolder = null;
            	if(dst != null){
    	        	dstFolder = Folder.get(builder, URI.create(dst));
    	        	if(dstFolder == null)
    	        		throw new BadRequestException("Target folder not exists. " + dst);
    	        	
    	        	destination = dstFolder.getPath();
    	        	if(destination.endsWith("/")){
    	        		destination = destination.substring(0, destination.length() - 1);
    	        	}
    	        	LOGGER.debug("destination: " + destination);
    	        	createdFolders.put(destination, dstFolder);
            	}
                
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
                    String name = entry.getName();
                    if(destination.trim().isEmpty() == false){
	                    if(name.startsWith("/")){
	                    	name = destination + name; 
	                    } else {
	                    	name = destination + "/" + name;
	                    }
                    }
                    addEntry(ro, name, zip.getInputStream(entry), createdFolders);
                    zip.getInputStream(entry).close();
                    if (roFromZipJobStatus.getProcessedResources() < roFromZipJobStatus.getSubmittedResources()) {
                        roFromZipJobStatus.setProcessedResources(roFromZipJobStatus.getProcessedResources() + 1);
                    }
                }
                                
                for (Entry<String, Folder> entryFolder : createdFolders.entrySet()){
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
							Folder f = ro.getFolders().get(ro.getUri().resolve(parentURI));
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
                if(dstFolder != null)
                	createdFolders.remove(destination);
            } catch (IOException | BadRequestException e) {
            	LOGGER.debug(e.getMessage(), e);
                throw new OperationFailedException("Can't preapre a ro from given zip", e);
            }
            status.setTarget(ro.getUri());
        } finally {
            builder.getEventBusModule().commit();
            if (started) {
                HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            }
            zipFile.delete();
        }
	}
	
}
