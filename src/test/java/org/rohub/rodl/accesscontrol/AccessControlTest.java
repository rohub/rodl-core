package org.rohub.rodl.accesscontrol;


import java.net.URI;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.rohub.rodl.accesscontrol.dicts.Mode;
import org.rohub.rodl.accesscontrol.dicts.Role;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.integration.AbstractIntegrationTest;
import org.rohub.rodl.integration.IntegrationTest;

import com.sun.jersey.api.client.ClientResponse;

@Category(IntegrationTest.class)
public class AccessControlTest extends AbstractIntegrationTest {

    protected final String userId2 = "http://" + UUID.randomUUID().toString();
    protected String accessToken2;
    protected UserProfile userProfile;
    protected UserProfile userProfile2;


    @Override
    @Before
    public void setUp()
            throws Exception {
        super.setUp();
        ClientResponse response = createUser(userId2, "test user");
        response = createAccessToken(clientId, userId2);
        accessToken2 = response.getLocation().resolve(".").relativize(response.getLocation()).toString();
        userProfile = new UserProfile(userId, userId, org.rohub.rodl.dl.UserMetadata.Role.AUTHENTICATED);
        userProfile2 = new UserProfile(userId2, userId2, org.rohub.rodl.dl.UserMetadata.Role.AUTHENTICATED);
    }


    @Override
    @After
    public void tearDown()
            throws Exception {
        super.tearDown();
    }


    protected ClientResponse grantPermission(URI ro, Role role, UserProfile user) {
        return grantPermission(adminCreds, ro, role, user);
    }

    protected ClientResponse grantPermission(String creds, URI ro, Role role, UserProfile user) {
        Permission handPermission = new Permission();
        handPermission.setRo(ro.toString());
        handPermission.setRole(role);
        handPermission.setUser(user);
        ClientResponse response = webResource.path("accesscontrol/permissions/").entity(handPermission)
                .type("application/json").accept("application/json").header("Authorization", "Bearer " + creds)
                .post(ClientResponse.class);
        return response;
    }

    protected ClientResponse changeMode(URI roUri, Mode mode) {
        return changeMode(adminCreds, roUri, mode);
    }

    protected ClientResponse changeMode(String creds, URI roUri, Mode mode) {
        AccessMode accessMode = new AccessMode();
        accessMode.setRo(roUri.toString());
        accessMode.setMode(mode);
        ClientResponse res = webResource.path("/accesscontrol/modes/").entity(accessMode).type("application/json")
                .header("Authorization", "Bearer " + creds).accept("application/json").post(ClientResponse.class);
        return res;
    }
}
