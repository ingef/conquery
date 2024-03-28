package com.bakdata.conquery.resources.unprotected;

import com.bakdata.conquery.apiv1.auth.JwtWrapper;
import com.bakdata.conquery.apiv1.auth.UsernamePasswordToken;
import com.bakdata.conquery.models.auth.basic.AccessTokenCreator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;

@Path("/")
@AllArgsConstructor
@Slf4j
public class TokenResource {

	private final AccessTokenCreator realm;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JwtWrapper getToken(UsernamePasswordToken token) {
		try {
			return new JwtWrapper(realm.createAccessToken(token.getUser(), token.getPassword()));
		}
		catch (AuthenticationException e) {
			log.warn("Failed to authorize request", e);
			throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
		}
	}
}
