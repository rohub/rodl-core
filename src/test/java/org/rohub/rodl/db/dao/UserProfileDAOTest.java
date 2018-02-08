package org.rohub.rodl.db.dao;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.AbstractUnitTest;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.AccessTokenDAO;
import org.rohub.rodl.db.dao.OAuthClientDAO;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.dl.UserMetadata.Role;

public class UserProfileDAOTest extends AbstractUnitTest {

    UserProfileDAO dao;
    UserProfile profile;
    String login = "login";
    String name = "name";
    Role role = Role.AUTHENTICATED;
    URI uri = URI.create("http://example.org/fakeUP/");


    @Override
    @Before
    public void setUp()
            throws Exception {
        super.setUp();
        dao = new UserProfileDAO();
        profile = new UserProfile(login, name, role, uri);
        dao.save(profile);
    }


    @Override
    @After
    public void tearDown()
            throws Exception {
        dao.delete(profile);
        super.tearDown();
    }


    @Test
    public void testConstructor() {
        UserProfileDAO daoT = new UserProfileDAO();
        Assert.assertNotNull(daoT);
    }


    @Test
    public void testFind() {
        Assert.assertEquals(profile, dao.findByLogin(profile.getLogin()));
    }


    @Test
    public void testDeleteCascade() {
        OAuthClientDAO oAuthClientDAO = new OAuthClientDAO();
        OAuthClient client = new OAuthClient(name, uri.toString());
        oAuthClientDAO.save(client);
        AccessTokenDAO tokenDAO = new AccessTokenDAO();
        AccessToken token1 = new AccessToken(client, profile);
        AccessToken token2 = new AccessToken(client, profile);
        tokenDAO.save(token1);
        tokenDAO.save(token2);
        List<AccessToken> tokens = new ArrayList<>();
        tokens.add(token1);
        tokens.add(token2);
        profile.setTokens(tokens);
        dao.save(profile);
        Assert.assertEquals(2, dao.findByLogin(profile.getLogin()).getTokens().size());
        Assert.assertNotNull(tokenDAO.findByValue(token1.getToken()));
        Assert.assertNotNull(tokenDAO.findByValue(token2.getToken()));
        dao.delete(profile);
        Assert.assertNull(tokenDAO.findByValue(token1.getToken()));
        Assert.assertNull(tokenDAO.findByValue(token2.getToken()));
        oAuthClientDAO.delete(client);
    }
}
