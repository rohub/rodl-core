package org.rohub.rodl.auth;



import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.AuthenticationException;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.spi.container.ContainerRequest;

public class UserProfileExistsFilter extends AuthorizationFilter {
	
	@Override
	public ContainerRequest filter(ContainerRequest request) {
		
		Object builder = this.httpRequest.getAttribute("Builder");
		if(builder == null){
			throw new MappableContainerException(
					new AuthenticationException("Unable to create user session.\n" +
							this.httpRequest.getAttribute(AUTH_MESSAGE), REALM));
		}
		
		return request;
	}

	@Override
	protected boolean isValid(String token) {
		return false;
	}

	@Override
	protected UserMetadata getUserProfile(String token) {
		return null;
	}
}
