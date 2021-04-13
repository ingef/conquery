package com.bakdata.conquery.models.auth.basic;

public interface AccessTokenCreator {

	/**
	 * Checks the provided basic authentication and creates an access token upon success.
	 *
	 * @return A valid access token that authenticates the user that provided the credentials
	 */
	String createAccessToken(String username, char[] password);

}