package com.bakdata.conquery.models.auth.oidc.keycloak;

import javax.ws.rs.core.Form;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

/**
 * Utility class for creating the form body needed to perform the client credential flow
 */
public class ClientCredentials {

	public static Form create(String clientId, String secret) {
		final MultivaluedStringMap map = new MultivaluedStringMap();

		map.add("grant_type", "client_credentials");
		map.add("client_id", clientId);
		map.add("client_secret", secret);

		return new Form(map);
	}
}
