package org.rohub.rodl.model.RO;


import java.net.URI;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.AbstractUnitTest;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.RO.Resource;
import org.rohub.rodl.vocabulary.RO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class ResourceTest extends AbstractUnitTest {

    private URI resourceUri;


    @Override
    @Before
    public void setUp()
            throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        resourceUri = researchObject.getUri().resolve("resource");
    }


    @Test
    public void testConstructor() {
        new Resource(userProfile, dataset, true, researchObject, resourceUri);
    }


    @Test
    public void testCreate() {
        Resource.create(builder, researchObject, resourceUri);
        Assert.assertTrue(researchObject.getResources().containsKey(resourceUri));
    }


    @Test
    public void testSave() {
        Resource resource = Resource.create(builder, researchObject, resourceUri);
        resource.save();
        Model model = ModelFactory.createDefaultModel();
        model.read(researchObject.getManifest().getGraphAsInputStream(RDFFormat.RDFXML), null);
        com.hp.hpl.jena.rdf.model.Resource r = model.getResource(resourceUri.toString());
        Assert.assertTrue(r.hasProperty(RDF.type, RO.Resource));
    }


    @Test
    public void testDelete() {
        Resource resource = Resource.create(builder, researchObject, resourceUri);
        resource.delete();
        Assert.assertFalse(researchObject.getResources().containsKey(resourceUri));
    }


    @Test
    @Ignore
    //FIXME this test is bad - it tests saving graph but has no content to save?
    public void testSaveGraphAndSerialize()
            throws BadRequestException {
        //TODO test serialization
        Resource resource = builder.buildResource(researchObject.getUri().resolve("resource.rdf"), researchObject,
            userProfile, DateTime.now());
        resource.save();
        resource.saveGraphAndSerialize();
        Model model = ModelFactory.createDefaultModel();
        model.read(researchObject.getGraphAsInputStream(RDFFormat.RDFXML), null);
        com.hp.hpl.jena.rdf.model.Resource r = model.getResource(resourceUri.toString());
        Assert.assertTrue(r.hasProperty(RDF.type, RO.Resource));
    }
}
