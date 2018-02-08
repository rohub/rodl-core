package org.rohub.rodl.accesscontrol.filters;


import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.rohub.rodl.accesscontrol.AccessControlTest;
import org.rohub.rodl.accesscontrol.dicts.Mode;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.integration.IntegrationTest;

import com.sun.jersey.api.client.ClientResponse;

@Category(IntegrationTest.class)
public class AccessControlModesResourceTest extends AccessControlTest {

    @Override
    @Before
    public void setUp()
            throws Exception {
        super.setUp();
    }


    @Override
    @After
    public void tearDown()
            throws Exception {
        super.tearDown();
    }


    @Test
    public void testIfOnlyOnwerCanReadAndWriteModes() {
        URI createdRO = createRO(accessToken);
        AccessMode mode = webResource.path("accesscontrol/modes").queryParam("ro", createdRO.toString())
                .header("Authorization", "Bearer " + adminCreds).get(AccessMode.class);
        Assert.assertEquals(createdRO.toString(), mode.getRo());
        Assert.assertEquals(Mode.PUBLIC, mode.getMode());
        //if user who isn't owner tires to change mode 
        //expect forbidden exception
        ClientResponse response = changeMode(accessToken2, createdRO, Mode.PRIVATE);
        Assert.assertEquals(400, response.getStatus());
        //if user who is owner tires to change mode
        //except that everything is fine
        response = changeMode(accessToken, createdRO, Mode.PRIVATE);
        Assert.assertEquals(201, response.getStatus());

        //only owner and admin can query
        response = webResource.path("accesscontrol/modes/").queryParam("ro", createdRO.toString())
                .header("Authorization", "Bearer " + adminCreds).get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        response = webResource.path("accesscontrol/modes/").queryParam("ro", createdRO.toString())
                .header("Authorization", "Bearer " + accessToken).get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        response = webResource.path("accesscontrol/modes/").queryParam("ro", createdRO.toString())
                .header("Authorization", "Bearer " + accessToken2).get(ClientResponse.class);
        Assert.assertEquals(403, response.getStatus());

        response = webResource.uri(mode.getUri()).header("Authorization", "Bearer " + adminCreds)
                .get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        response = webResource.uri(mode.getUri()).header("Authorization", "Bearer " + accessToken)
                .get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        response = webResource.uri(mode.getUri()).header("Authorization", "Bearer " + accessToken2)
                .get(ClientResponse.class);
        Assert.assertEquals(403, response.getStatus());

    }

}
