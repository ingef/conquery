package com.bakdata.conquery.resources.unprotected;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.auth.JwtWrapper;
import com.bakdata.conquery.apiv1.auth.UsernamePasswordToken;
import com.bakdata.conquery.models.auth.basic.AccessTokenCreator;
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
