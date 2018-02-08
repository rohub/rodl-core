package org.rohub.rodl.monitoring;


import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rohub.rodl.db.dao.ResearchObjectPreservationStatusDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.monitoring.PreservationJob;
import org.rohub.rodl.preservation.ResearchObjectPreservationStatus;
import org.rohub.rodl.preservation.Status;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;

public class PreservationJobTest {

    JobExecutionContext context;
    URI checklistNotificationsUri;

    Dataset dataset;
    UserMetadata userProfile;
    Builder builder;
    URI roUri = URI.create("http://www.example.com/ROs/preservationJobTest/" + UUID.randomUUID().toString() + "/");
    ResearchObjectPreservationStatusDAO preservationDAO;


    @Before
    public void setUp()
            throws IOException {
        context = Mockito.mock(JobExecutionContext.class);
        Properties properties = new Properties();
        dataset = DatasetFactory.createMem();
        userProfile = new UserMetadata("jank", "Jan Kowalski", Role.AUTHENTICATED, URI.create("http://jank"));
        builder = new Builder(userProfile, dataset, false);
        preservationDAO = new ResearchObjectPreservationStatusDAO();
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
    }


    public void tearDown() {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
    }


    @Ignore
    @Test
    public void testJobExecute()
            throws JobExecutionException, IOException, InterruptedException {
        ResearchObject.create(builder, roUri);
        ResearchObjectPreservationStatus status = preservationDAO.findById(roUri.toString());
        Assert.assertEquals(Status.NEW, status.getStatus());
        //prepare job
        PreservationJob job = new PreservationJob();
        job.setBuilder(builder);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(PreservationJob.RESEARCH_OBJECT_URI, roUri);
        context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        job.execute(context);
    }

}
