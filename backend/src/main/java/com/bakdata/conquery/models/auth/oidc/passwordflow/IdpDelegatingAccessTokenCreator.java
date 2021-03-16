package com.bakdata.conquery.models.auth.oidc.passwordflow;

import com.bakdata.conquery.models.auth.basic.AccessTokenCreator;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealmFactory;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;


@Slf4j
@Getter
@Setter
public class IdpDelegatingAccessTokenCreator implements AccessTokenCreator {

	private static final String GROUPS_CLAIM = "groups";

	private final IntrospectionDelegatingRealmFactory authProviderConf;


	public IdpDelegatingAccessTokenCreator(IntrospectionDelegatingRealmFactory authProviderConf) {
		this.authProviderConf = authProviderConf;
	}

	@Override
	@SneakyThrows
	public String createAccessToken(String username, char[] password) {
		
		Secret passwordSecret = new Secret(new String(password));

		AuthorizationGrant  grant = new ResourceOwnerPasswordCredentialsGrant(username, passwordSecret);
		
		URI tokenEndpoint =  UriBuilder.fromUri(authProviderConf.getTokenEndpoint()).build();

		TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, authProviderConf.getClientAuthentication(), grant, Scope.parse("openid"));
		
		
		TokenResponse response = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

		if (!response.indicatesSuccess()) {
			HTTPResponse httpResponse = response.toHTTPResponse();
			log.error("Received the following error from the auth server while validating username and password:\n\tPath: {}\n\tStatus code: {}\n\tStatus message: {}\n\tContent: {}", tokenEndpoint, httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getContent());
			throw new IllegalStateException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof AccessTokenResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new IllegalStateException("Unknown token response. See log.");
		}

		AccessTokenResponse successResponse = (AccessTokenResponse) response;

		// Get the access token, the server may also return a refresh token
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		//RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
		return accessToken.getValue();
	}

}
