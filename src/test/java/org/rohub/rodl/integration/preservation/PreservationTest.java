package org.rohub.rodl.integration.preservation;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.rohub.rodl.db.dao.ResearchObjectPreservationStatusDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.integration.AbstractIntegrationTest;
import org.rohub.rodl.integration.IntegrationTest;
import org.rohub.rodl.preservation.ResearchObjectPreservationStatus;
import org.rohub.rodl.preservation.Status;

import com.sun.jersey.api.client.ClientResponse;

@Ignore
@Category(IntegrationTest.class)
public class PreservationTest extends AbstractIntegrationTest {

    int SINGLE_BREAK = 10;
    int MAX_PAUSES_NUMBER = 100;
    private ResearchObjectPreservationStatusDAO dao = new ResearchObjectPreservationStatusDAO();


    @Test
    public void testCreateAndPresarve()
            throws IOException, InterruptedException {
        URI cretedRO = createRO();
        ResearchObjectPreservationStatus preservationStatus = dao.findById(cretedRO.toString());
        Assert.assertEquals(Status.NEW, preservationStatus.getStatus());

    }


    @Test
    public void testUpdateAndPreserver()
            throws IOException, InterruptedException {
        String filePath = "added/file";
        URI cretedRO = createRO();
        ClientResponse response = addLoremIpsumFile(cretedRO, filePath);
        ResearchObjectPreservationStatus preservationStatus = dao.findById(cretedRO.toString());
        Assert.assertEquals(Status.NEW, preservationStatus.getStatus());
    }


    @Test
    public void testAnnotateAndPreserve()
            throws InterruptedException, IOException {
        String annotationBodyPath = "annotation/body";
        URI cretedRO = createRO();
        InputStream is = getClass().getClassLoader().getResourceAsStream("rdfStructure/mess-ro/.ro/annotationGood.rdf");
        ClientResponse response = addAnnotation(cretedRO, annotationBodyPath, is);
        IOUtils.closeQuietly(is);
        ResearchObjectPreservationStatus preservationStatus = dao.findById(cretedRO.toString());
        Assert.assertEquals(Status.NEW, preservationStatus.getStatus());
    }


    @Test
    public void testUpdateSaveAndUpdate()
            throws IOException, InterruptedException {
        String filePath = "added/file";
        URI cretedRO = createRO();
        ClientResponse response = addLoremIpsumFile(cretedRO, filePath);
        response = webResource.uri(cretedRO).path(filePath).header("Authorization", "Bearer " + accessToken)
                .type("text/plain").put(ClientResponse.class, "lorem ipsum");
        ResearchObjectPreservationStatus preservationStatus = dao.findById(cretedRO.toString());
        preservationStatus.setStatus(Status.UP_TO_DATE);
        dao.save(preservationStatus);
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        response = webResource.uri(cretedRO).path(filePath).header("Authorization", "Bearer " + accessToken)
                .type("text/plain").put(ClientResponse.class, "new content");
        preservationStatus = dao.findById(cretedRO.toString());
        Assert.assertEquals(Status.UPDATED, preservationStatus.getStatus());
    }

}
