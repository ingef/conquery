package com.bakdata.conquery.models.auth.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.basic.BasicAuthRealm;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.shiro.authc.UsernamePasswordToken;

@Path("/")
@AllArgsConstructor
public class TokenResource {

	private final BasicAuthRealm realm;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TokenWrapper getToken(UsernamePasswordToken token) {
		return new TokenWrapper(realm.checkCredentialsAndCreateJWT(token.getUsername(), token.getPassword()));
	}

	@GET
	public String getToken() {
		return "Hello";
	}

	@AllArgsConstructor
	@Data
	private static class TokenWrapper {

		private String access_token;
	}
}
