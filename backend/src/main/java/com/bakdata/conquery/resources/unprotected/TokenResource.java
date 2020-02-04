package com.bakdata.conquery.resources.unprotected;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.auth.JwtWrapper;
import com.bakdata.conquery.apiv1.auth.UsernamePasswordToken;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import lombok.AllArgsConstructor;

@Path("/")
@AllArgsConstructor
public class TokenResource {

	private final LocalAuthenticationRealm realm;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JwtWrapper getToken(UsernamePasswordToken token) {
		return new JwtWrapper(realm.checkCredentialsAndCreateJWT(token.getUser(), token.getPassword()));
	}
}
