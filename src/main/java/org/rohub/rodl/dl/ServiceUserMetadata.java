package org.rohub.rodl.dl;


import java.net.URI;

import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;

public class ServiceUserMetadata {

	public static final UserMetadata RODL = new UserMetadata(
						"http://rohub.org/users/rohub-service/", 
						"ROHUB Service", 
						Role.ADMIN,
						URI.create("http://rohub.org/users/rohub-service/")
						);
}
