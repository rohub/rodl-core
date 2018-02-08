package org.rohub.rodl.db;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.UserProfile;

public class AccessTokenTest {

    OAuthClient client;
    UserProfile profile;
    String token;


    @Before
    public void setUp() {
        client = new OAuthClient();
        profile = new UserProfile();
        token = "token";
    }


    @Test
    public void testConstuctor() {
        AccessToken accessToken = new AccessToken(token, client, profile);
        Assert.assertEquals(token, accessToken.getToken());
        Assert.assertEquals(client, accessToken.getClient());
        Assert.assertEquals(profile, accessToken.getUser());
    }
}
