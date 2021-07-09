package com.bakdata.conquery.models.auth.web;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.keycloak.authorization.client.AuthzClient;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.net.URI;

@Slf4j
@RequiredArgsConstructor
public class OAuthRedirectFilter  implements ContainerRequestFilter, ContainerResponseFilter {

    private final URI tokenEndpoint;

    @SneakyThrows
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Cookie accessTokenCookie = requestContext.getCookies().get(AuthCookieFilter.ACCESS_TOKEN);
        if (accessTokenCookie != null) {
            return;
        }
        final AuthenticationResponse authResponse = AuthenticationResponseParser.parse(requestContext.getUriInfo().getRequestUri());

        if (!authResponse.indicatesSuccess()) {
            log.warn("Authentication response was not successful: {}",authResponse.toErrorResponse());
            return;
        }
        final AuthenticationSuccessResponse authenticationSuccessResponse = authResponse.toSuccessResponse();

        final TokenRequest tokenRequest = new TokenRequest(
                tokenEndpoint,
                new AuthorizationCodeGrant(
                        authenticationSuccessResponse.getAuthorizationCode(),
                        authenticationSuccessResponse.getRedirectionURI()
                )
        );


        TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

        if (! tokenResponse.indicatesSuccess()) {
            log.warn("Token response was not successful: {}",tokenResponse.toErrorResponse());
            return;
        }

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

    }
}
