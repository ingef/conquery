package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Path("/authenticate")
@Slf4j
public class AuthenticationCallbackResource {

    @Context
    ContainerRequestContext context;

    private URI tokenEndpoint;

    @GET
    @SneakyThrows
    public Response redeemAuthCode(){

        final AuthenticationResponse authResponse = AuthenticationResponseParser.parse(context.getUriInfo().getRequestUri());

        if (!authResponse.indicatesSuccess()) {
            throw new BadRequestException();
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
            throw new BadRequestException();
        }

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();


        final AuthState authState = AuthState.decode(authResponse.getState().getValue());

        return Response.seeOther(authState.getOriginalRequest())
                .cookie(
                    new NewCookie(AuthCookieFilter.ACCESS_TOKEN,
                            accessToken.getValue(),
                            null,
                            null,
                            0,
                            null,
                            AuthCookieFilter.COOKIE_MAX_AGE_HOURS,
                            null,
                            false,
                            false)
                ).build();

    }


    @Data
    public static class AuthState {

        private URI originalRequest;

        @SneakyThrows
        public static AuthState decode(String encoded) {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return Jackson.MAPPER.readerFor(AuthState.class).readValue(decoded);
        }

        @SneakyThrows
        public String encode() {
            final byte[] decoded = Jackson.MAPPER.writerFor(AuthState.class).writeValueAsBytes(this);
            return Base64.getEncoder().encodeToString(decoded);
        }

    }
}
