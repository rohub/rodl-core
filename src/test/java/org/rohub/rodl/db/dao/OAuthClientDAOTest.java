package org.rohub.rodl.db.dao;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.AbstractUnitTest;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.dao.OAuthClientDAO;

public class OAuthClientDAOTest extends AbstractUnitTest {

    OAuthClientDAO dao;


    @Override
    @Before
    public void setUp()
            throws Exception {
        super.setUp();
        dao = new OAuthClientDAO();
    }


    @Test
    public void testConstructor() {
        OAuthClientDAO daoT = new OAuthClientDAO();
        Assert.assertNotNull(daoT);
    }


    @Test
    public void testFindAll() {
        OAuthClient client1 = new OAuthClient("name", "http://www.example.com/redirect");
        OAuthClient client2 = new OAuthClient("name", "http://www.example.com/redirect");
        Assert.assertFalse(dao.findAll().contains(client1));
        Assert.assertFalse(dao.findAll().contains(client2));
        dao.save(client1);
        dao.save(client2);
        Assert.assertTrue(dao.findAll().contains(client1));
        Assert.assertTrue(dao.findAll().contains(client2));
        dao.delete(client2);
        dao.delete(client1);
    }


    @Test
    public void testFIndById() {
        OAuthClient client1 = new OAuthClient("name", "http://www.example.com/redirect");
        OAuthClient client2 = new OAuthClient("name", "http://www.example.com/redirect");
        Assert.assertNull(dao.findById(client1.getClientId()));
        Assert.assertNull(dao.findById(client2.getClientId()));
        dao.save(client1);
        dao.save(client2);
        Assert.assertEquals(client1, dao.findById(client1.getClientId()));
        Assert.assertEquals(client2, dao.findById(client2.getClientId()));
        dao.delete(client2);
        dao.delete(client1);

    }
}
