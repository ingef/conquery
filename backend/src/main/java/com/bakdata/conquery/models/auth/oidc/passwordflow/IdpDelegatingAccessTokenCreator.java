package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.models.auth.basic.AccessTokenCreator;
import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class IdpDelegatingAccessTokenCreator implements AccessTokenCreator {

	private final IntrospectionDelegatingRealmFactory authProviderConf;

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
