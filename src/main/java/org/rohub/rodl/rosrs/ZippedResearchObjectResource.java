package org.rohub.rodl.rosrs;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;

import com.sun.jersey.core.header.ContentDisposition;

/**
 * A REST API resource corresponding to a zipped RO.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Path("zippedROs/{ro_id}/")
public class ZippedResearchObjectResource {

    /** URI info. */
    @Context
    UriInfo uriInfo;

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ZippedResearchObjectResource.class);


    /**
     * Returns zip archive with contents of RO version.
     * 
     * @param researchObjectId
     *            RO identifier - defined by the user
     * @return 200 OK
     * @throws DigitalLibraryException
     *             could not get the RO frol dLibra
     * @throws NotFoundException
     *             could not get the RO frol dLibra
     */
    @SuppressWarnings("resource")
    @GET
    @Produces({ "application/zip", "multipart/related" })
    public Response getZippedRO(@PathParam("ro_id") String researchObjectId)
            throws DigitalLibraryException, NotFoundException {
        URI uri = URI.create(uriInfo.getAbsolutePath().toString().replaceFirst("zippedROs", "ROs"));
        ResearchObject researchObject = ResearchObject.get(builder, uri);
        if (researchObject == null) {
            throw new NotFoundException("Research Object not found");
        }
        //TODO add all named graphs from SMS that start with the base URI
        InputStream body = researchObject.getAsZipArchive();
        File tmpZipFile = null;
        File tmpZipFileOut = null;
        ZipOutputStream zipOutputStream = null;
        FileInputStream resultInputStream = null;
        try {
            tmpZipFile = File.createTempFile("zippedROIn", ".zip");
            tmpZipFileOut = File.createTempFile("zippedROut", ".zip");
            tmpZipFile.delete();
            tmpZipFileOut.delete();
            tmpZipFile.deleteOnExit();
            tmpZipFileOut.deleteOnExit();
            zipOutputStream = new ZipOutputStream(new FileOutputStream(tmpZipFileOut));
            IOUtils.copy(body, new FileOutputStream(tmpZipFile));
            ZipFile zipFile = new ZipFile(tmpZipFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry originalEntry = entries.nextElement();
                if (originalEntry.getName().equals(ResearchObject.MANIFEST_PATH)) {
                    ZipEntry manifest = new ZipEntry(originalEntry.getName());
                    zipOutputStream.putNextEntry(manifest);
                    researchObject.getManifest().addAuthorsName(zipOutputStream, researchObject.getUri(),
                        RDFFormat.RDFXML);
                } else {
                    zipOutputStream.putNextEntry(originalEntry);
                    IOUtils.copy(zipFile.getInputStream(originalEntry), zipOutputStream);
                }
            }
            zipOutputStream.close();
            resultInputStream = new FileInputStream(tmpZipFileOut);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Can't zip " + researchObject.getUri().toString(), e);
        }
        tmpZipFile.delete();
        tmpZipFileOut.delete();
        ContentDisposition cd = ContentDisposition.type("attachment").fileName(researchObjectId + ".zip").build();
        return ResearchObjectResource.addLinkHeaders(Response.ok(resultInputStream), uriInfo, researchObjectId)
                .header("Content-disposition", cd).type("application/zip").build();
    }
}
