package org.rohub.rodl.db;


import org.junit.Assert;
import org.junit.Test;
import org.rohub.rodl.db.OAuthClient;

public class OAuthClientTest {

    String id = "id";
    String name = "name";
    String uri = "http://www.example.com/some-clinet-uri";


    @Test
    public void testConstructor() {
        OAuthClient client = new OAuthClient(id, name, uri);
        Assert.assertEquals(id, client.getClientId());
        Assert.assertEquals(name, client.getName());
        Assert.assertEquals(uri, client.getRedirectionURI());
    }
}
