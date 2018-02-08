package org.rohub.rodl.integration.rosrs;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.EntityTag;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.integration.IntegrationTest;
import org.rohub.rodl.vocabulary.ORE;
import org.rohub.rodl.vocabulary.RO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.sun.jersey.api.client.ClientResponse;

@Category(IntegrationTest.class)
public class FileTest extends RosrsTest {

    private final String filePath = "foo/bar ra.txt";
    private final String filePathEncoded = "foo/bar%20ra.txt";
    private final String rdfFilePath = "foo/bar.rdf";
    private String rdfFileBody = "<rdf:RDF" + "  xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n"
            + "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n" + "   <ore:Proxy>\n"
            + "   </ore:Proxy>\n" + " </rdf:RDF>";


    @Test
    public void testAddAndGetFile() {
        ClientResponse response = webResource.uri(ro).path(filePath).header("Authorization", "Bearer " + accessToken)
                .delete(ClientResponse.class);
        response.close();

        DateTime addFileTime = new DateTime();
        response = addLoremIpsumFile(ro, filePath);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        assertNotNull(response.getLastModified());
        //        assertTrue(!new DateTime(response.getLastModified()).isBefore(addFileTime));
        assertNotNull(response.getEntityTag());
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        model.read(response.getEntityInputStream(), null);
        Individual proxy = model.getIndividual(response.getLocation().toString());
        Individual resource = model.getIndividual(ro.resolve(filePathEncoded).toString());
        Assert.assertTrue(proxy.hasRDFType(ORE.Proxy));
        Assert.assertTrue(resource.hasRDFType(RO.Resource));
        Assert.assertTrue(model.contains(proxy, ORE.proxyFor, resource));
        Assert.assertTrue(model.contains(resource, DCTerms.created, (RDFNode) null));
        Assert.assertTrue(model.contains(resource, DCTerms.creator, (RDFNode) null));
        response.close();

        response = webResource.uri(ro).path(filePathEncoded).header("Authorization", "Bearer " + accessToken)
                .type("text/plain").put(ClientResponse.class, "lorem ipsum");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNotNull(response.getLastModified());
        //        assertTrue(!new DateTime(response.getLastModified()).isBefore(addFileTime));
        assertNotNull(response.getEntityTag());
        response.close();

        String content = webResource.uri(ro).path(filePathEncoded).header("Authorization", "Bearer " + accessToken)
                .get(String.class);
        assertTrue(content.contains("lorem ipsum"));

        response = webResource.uri(ro).path(filePath).header("Authorization", "Bearer " + accessToken).head();
        assertNotNull(response.getLastModified());
        //        assertFalse(new DateTime(response.getLastModified()).isBefore(addFileTime));
        assertNotNull(response.getEntityTag());
        response.close();
    }


    @Ignore
    //this test doesn't make sense
    @Test
    public void testAddAndGetRDFFile() {
        DateTime addRdfFileTime = new DateTime();
        ClientResponse response = addFile(ro, rdfFilePath, IOUtils.toInputStream(rdfFileBody), "application/rdf+xml");
        assertEquals("RDF file should be created correctly via post method", HttpServletResponse.SC_CREATED,
            response.getStatus());
        response.close();

        response = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken)
                .type(RDFFormat.RDFXML.getDefaultMIMEType()).put(ClientResponse.class, "lorem ipsum");
        assertEquals("RDF file should be updated correctly via put method", HttpServletResponse.SC_OK,
            response.getStatus());
        response.close();

        String metadata = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken)
                .get(String.class);
        assertTrue(metadata.contains("lorem ipsum"));

        response = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken).head();
        Date lastModified = response.getLastModified();
        assertNotNull(lastModified);
        //        assertFalse(new DateTime(lastModified).isBefore(addRdfFileTime));
        EntityTag tag = response.getEntityTag();
        assertNotNull(tag);
        response.close();

        response = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken)
                .header("If-None-Match", tag).head();
        assertEquals(HttpStatus.SC_NOT_MODIFIED, response.getStatus());
        response.close();

        response = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken)
                .header("If-Modified-Since", lastModified).head();
        assertEquals(HttpStatus.SC_NOT_MODIFIED, response.getStatus());
        response.close();
    }


    @Test
    public void deleteFile() {
        addLoremIpsumFile(ro, filePath);
        ClientResponse response = webResource.uri(ro).path(filePath).header("Authorization", "Bearer " + accessToken)
                .delete(ClientResponse.class);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        response.close();
    }


    @Test
    public void deleteRDFFile() {
        ClientResponse response = addFile(ro, rdfFilePath, IOUtils.toInputStream(rdfFileBody), "application/rdf+xml");
        URI rdfProxy = response.getLocation();
        response = webResource.uri(rdfProxy).header("Authorization", "Bearer " + accessToken)
                .type(RDFFormat.RDFXML.getDefaultMIMEType()).delete(ClientResponse.class);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        response.close();

        response = webResource.uri(rdfProxy).header("Authorization", "Bearer " + accessToken).get(ClientResponse.class);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        response.close();

        response = webResource.uri(ro).path(rdfFilePath).header("Authorization", "Bearer " + accessToken)
                .get(ClientResponse.class);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        response.close();
    }

}
