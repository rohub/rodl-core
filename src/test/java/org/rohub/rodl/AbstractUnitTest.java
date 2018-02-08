package org.rohub.rodl;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.rohub.rodl.db.dao.ResearchObjectIdDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.eventbus.SimpleEventBusModule;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

/**
 * Base test class for model package.
 * 
 */
public abstract class AbstractUnitTest {

    /** Temporary in-memory dataset. */
    protected Dataset dataset;

    /** Test user. */
    protected static UserMetadata userProfile;

    /** Builder using the temporary dataset and the test user. */
    protected Builder builder;

    /** RO URI as String, mapped using location-mapping.n3 to a local file. */
    protected static final String RESEARCH_OBJECT = "http://example.org/ro-1/";

    /** RO URI as String, mapped using location-mapping.n3 to a local file. */
    protected static final String RESEARCH_OBJECT_2 = "http://example.org/ro-2/";

    /** Manifest URI as String, mapped. */
    protected static final String MANIFEST = "http://example.org/ro-1/.ro/manifest.rdf";

    /** Manifest URI as String, mapped. */
    protected static final String MANIFEST_2 = "http://example.org/ro-2/.ro/manifest.rdf";

    /** Annotation body URI as String, mapped. */
    protected static final String ANNOTATION_BODY = "http://example.org/ro-1/.ro/annotationBody.ttl";

    /** Resource URI as String, mapped. */
    protected static final String RESOURCE1 = "http://example.org/ro-1/a%20workflow.t2flow";

    /** Resource URI as String, mapped. */
    protected static final String RESOURCE2 = "http://workflows.org/a%20workflow.scufl";

    /** Folder resource map URI as String, mapped. */
    protected static final String FOLDER_RESOURCE_MAP = "http://example.org/ro-1/folder-rm.ttl";

    /** Folder resource map URI as String, mapped. */
    protected static final String FOLDER_RESOURCE_MAP_2 = "http://example.org/ro-2/folder-rm.ttl";

    /** Empty RO. */
    protected ResearchObject researchObject;
    /** Empty RO. */
    protected ResearchObject researchObject2;


    /**
     * Create the dataset, load the RDF files.
     * 
     * @throws Exception
     */
    @Before
    public void setUp()
            throws Exception {
        Model model;
        dataset = DatasetFactory.createMem();
        userProfile = new UserMetadata("jank", "Jan Kowalski", Role.AUTHENTICATED, URI.create("http://jank"));
        builder = new Builder(userProfile, dataset, false);
        // don't want to wait until the end of HTTP request to commit.
        builder.setEventBusModule(new SimpleEventBusModule());
        researchObject = builder.buildResearchObject(URI.create(RESEARCH_OBJECT));
        model = FileManager.get().loadModel(MANIFEST_2, MANIFEST_2, "RDF/XML");
        dataset.addNamedModel(MANIFEST_2, model);
        researchObject2 = builder.buildResearchObject(URI.create(RESEARCH_OBJECT_2));
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();

        model = FileManager.get().loadModel(MANIFEST, MANIFEST, "RDF/XML");
        dataset.addNamedModel(MANIFEST, model);
        serializeModelFile(model, researchObject.getUri(), MANIFEST, "TURTLE");

        model = FileManager.get().loadModel(ANNOTATION_BODY, ANNOTATION_BODY, "TURTLE");
        dataset.addNamedModel(ANNOTATION_BODY, model);
        serializeModelFile(model, researchObject.getUri(), ANNOTATION_BODY, "TURTLE");

        model = FileManager.get().loadModel(FOLDER_RESOURCE_MAP, FOLDER_RESOURCE_MAP, "TURTLE");
        dataset.addNamedModel(FOLDER_RESOURCE_MAP, model);
        serializeModelFile(model, researchObject.getUri(), FOLDER_RESOURCE_MAP, "TURTLE");

        model = FileManager.get().loadModel(FOLDER_RESOURCE_MAP_2, FOLDER_RESOURCE_MAP_2, "TURTLE");
        dataset.addNamedModel(FOLDER_RESOURCE_MAP_2, model);
        serializeModelFile(model, researchObject.getUri(), FOLDER_RESOURCE_MAP_2, "TURTLE");
    }


    private void serializeModelFile(Model model, URI researchObjectUri, String filePath, String lang) {
        URI filePathUri = URI.create(filePath);
        if (filePathUri.isAbsolute()) {
            filePathUri = researchObjectUri.relativize(filePathUri);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.write(out, lang);
        //builder.getDigitalLibrary().createOrUpdateFile(researchObjectUri, filePathUri.toString(),
        //    new ByteArrayInputStream(out.toByteArray()), mimeType);

        builder.getDigitalLibrary().createOrUpdateFile(researchObjectUri, "folder-rm.ttl",
            IOUtils.toInputStream("nic"), "text/plain");
    }


    /**
     * Close the dataset.
     * 
     * @throws Exception
     *             when the filesystem can't be accessed
     */
    @After
    public void tearDown()
            throws Exception {
        builder.getDataset().close();
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
    }


    /**
     * Check if the model contains a triple.
     * 
     * @param model
     *            model that should contain the triple
     * @param subjectURI
     *            triple subject
     * @param propertyURI
     *            triple property
     * @param object
     *            triple object (a literal)
     */
    protected void verifyTriple(Model model, URI subjectURI, URI propertyURI, String object) {
        Resource subject = model.createResource(subjectURI.toString());
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    /**
     * Check if the model contains a triple.
     * 
     * @param model
     *            model that should contain the triple
     * @param subjectURI
     *            triple subject as String
     * @param propertyURI
     *            triple property
     * @param object
     *            triple object (a literal)
     */
    protected void verifyTriple(Model model, String subjectURI, URI propertyURI, String object) {
        Resource subject = model.createResource(subjectURI);
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    /**
     * Check if the model contains a triple.
     * 
     * @param model
     *            model that should contain the triple
     * @param subjectURI
     *            triple subject as String
     * @param propertyURI
     *            triple property
     * @param object
     *            triple object (a resource)
     */
    protected void verifyTriple(Model model, String subjectURI, URI propertyURI, Resource object) {
        Resource subject = model.createResource(subjectURI);
        Property property = model.createProperty(propertyURI.toString());
        Assert.assertTrue(String.format("Annotation body must contain a triple <%s> <%s> <%s>", subject.getURI(),
            property.getURI(), object), model.contains(subject, property, object));
    }


    protected void clearDLFileSystem() {
        Properties properties = new Properties();
        String filesystemBase = properties.getProperty("filesystemBase", "/tmp/dl/");
        File directory = new File(filesystemBase);
        if (directory.listFiles() != null) {
            for (File f : directory.listFiles()) {
                removeDirectory(f);
            }
        }
    }


    private boolean removeDirectory(File directory) {
        if (directory == null) {
            return false;
        }
        if (!directory.exists()) {
            return true;
        }
        if (!directory.isDirectory()) {
            return false;
        }
        String[] list = directory.list();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                File entry = new File(directory, list[i]);
                if (entry.isDirectory()) {
                    if (!removeDirectory(entry)) {
                        return false;
                    }
                } else {
                    if (!entry.delete()) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }


    protected void deleteResearchObjectIds(URI uri) {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
        ResearchObjectIdDAO dao = new ResearchObjectIdDAO();
        if (dao.findByPrimaryKey(uri) != null) {
            dao.delete(dao.findByPrimaryKey(uri));
        }
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
    }
}
