package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.apiv1.RequestHelper;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.oidc.JwtPkceVerifyingRealm;
import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.dropwizard.validation.ValidationMethod;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.representations.AccessToken;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.PublicKey;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A realm that verifies oauth tokens using PKCE.
 */
@CPSType(id = "JWT_PKCE_REALM", base = AuthenticationRealmFactory.class)
@NoArgsConstructor
@Data
@Slf4j
public class JwtPkceVerifyingRealmFactory implements AuthenticationRealmFactory {

    /**
     * The client id is also used as the expected audience in the validated token.
     * Ensure that the IDP is configured accordingly.
     */
    @NonNull
    private String client;
    @NotNull
    private List<String> additionalTokenChecks = Collections.emptyList();

    /**
     * Either the wellKnownEndpoint from which an idpConfiguration can be obtained or the idpConfiguration must be supplied.
     * If the idpConfiguration is given, the wellKnownEndpoint is ignored.
     */
    private URI wellKnownEndpoint;

    /**
     * See wellKnownEndpoint.
     */
    private IdpConfiguration idpConfiguration;

    /**
     * A leeway for token's expiration in seconds, this should be a short time.
     *
     * One Minute is the default.
     */
    @Min(0)
    private int tokenLeeway = 60;


    /**
     * Which claims hold alternative Ids of the user in case the user name does not match a user.
     * Pay attention, that the user must not be able to alter the value of any of these claims.
     */
    private List<String> alternativeIdClaims = Collections.emptyList();


    @JsonIgnore
    private Supplier<Optional<IdpConfiguration>> idpConfigurationSupplier;

    /**
     * Authentication cookie creator for using the Admin API
     */
    @JsonIgnore
    public BiFunction<ContainerRequestContext, String, Cookie> authCookieCreator;



    @ValidationMethod(message = "Neither wellKnownEndpoint nor idpConfiguration was given")
    @JsonIgnore
    public boolean isConfigurationAvailable() {
        return wellKnownEndpoint != null || idpConfiguration != null;
    }

    @AllArgsConstructor
    @Getter
    public static class IdpConfiguration {

        /**
         * The public key information that is used to validate signed JWT.
         * It can be retrieved from the IDP.
         */
        @NonNull
        private final PublicKey publicKey;

        @NonNull
        private final URI authorizationEndpoint;

        @NonNull
        private final URI tokenEndpoint;

        @NotEmpty
        private final String issuer;
    }

    public ConqueryAuthenticationRealm createRealm(ManagerNode manager) {
        List<TokenVerifier.Predicate<AccessToken>> additionalVerifiers = new ArrayList<>();

        for (String additionalTokenCheck : additionalTokenChecks) {
            additionalVerifiers.add(ScriptedTokenChecker.create(additionalTokenCheck));
        }

        idpConfigurationSupplier = getIdpOptionsSupplier(manager.getClient());
        authCookieCreator = manager.getConfig().getAuthentication()::createAuthCookie;

        // Add login schema for admin end
        final RedirectingAuthFilter redirectingAuthFilter = manager.getAuthController().getRedirectingAuthFilter();
        redirectingAuthFilter.getAuthAttemptCheckers().add(this::checkForAuthCallback);
        redirectingAuthFilter.getLoginInitiators().add(this::initiateLogin);

        return new JwtPkceVerifyingRealm(idpConfigurationSupplier, client, additionalVerifiers, alternativeIdClaims, tokenLeeway);
    }

    @Data
    private static class JWKs {
        List<JWK> keys;
    }

    private Supplier<Optional<IdpConfiguration>> getIdpOptionsSupplier(final Client client) {

        return () -> {
            if (idpConfiguration == null) {
                synchronized (this) {
                    // check again since we are now in an exclusive section
                    if (idpConfiguration == null) {
                        // retrieve the configuration and cache it
                        idpConfiguration = retrieveIdpConfiguration(client);
                    }
                }
            }
            return Optional.ofNullable(idpConfiguration);
        };
    }

    /**
     * Retrieve the configuration of the IDP from its "wellknown"-endpoint.
     * @implNote Since this involves https requests, ensure to cache the returned value.
     */
    private IdpConfiguration retrieveIdpConfiguration(final Client client) {

        if (wellKnownEndpoint == null) {
            log.error("Cannot retrieve idp configuration, because no well-known endpoint was given");
            return null;
        }

        JsonNode response = null;
        try {
            response = client.target(wellKnownEndpoint)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (Exception e) {
            log.warn("Unable to retrieve configuration from {}", wellKnownEndpoint, e);
            return null;
        }

        if (response == null) {
            return null;
        }

        String issuer = response.get("issuer").asText();
        URI authorizationEndpoint = URI.create(response.get("authorization_endpoint").asText());
        URI tokenEndpoint = URI.create(response.get("token_endpoint").asText());

        URI jwksUri = URI.create(response.get("jwks_uri").asText());

        JWKs jwks = null;
        try {
            jwks = client.target(jwksUri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(JWKs.class);

        } catch (Exception e) {
            log.warn("Unable to retrieve jwks from {}", wellKnownEndpoint, e);
            return null;
        }

        if (jwks == null) {
            return null;
        }


        final List<JWK> keys = jwks.getKeys();
        if (keys.size() != 1) {
            throw new IllegalStateException("Expected exactly 1 jwk for realm but found: " + keys.size());
        }

        JWK jwk = keys.get(0);

        return new IdpConfiguration(getPublicKey(jwk), authorizationEndpoint, tokenEndpoint, issuer);
    }


    @JsonIgnore
    @SneakyThrows(JsonProcessingException.class)
    private static PublicKey getPublicKey(JWK jwk) {
        // We have to re-serdes the object because it might be a sub class which can not be handled correctly by the JWKParser
        String jwkString = Jackson.MAPPER.writeValueAsString(jwk);
        return JWKParser.create().parse(jwkString).toPublicKey();
    }

    public static abstract class ScriptedTokenChecker extends Script implements TokenVerifier.Predicate<AccessToken> {

        private final static GroovyShell SHELL;

        static {
            CompilerConfiguration config = new CompilerConfiguration();
            config.addCompilationCustomizers(new ImportCustomizer().addImports(AccessToken.class.getName()));
            config.setScriptBaseClass(ScriptedTokenChecker.class.getName());

            SHELL = new GroovyShell(config);
        }

        public static ScriptedTokenChecker create(String checkScript) {


            return (ScriptedTokenChecker) SHELL.parse(checkScript);
        }

        @Override
        public abstract Boolean run();

        @Override
        public boolean test(AccessToken token) throws VerificationException {
            Binding binding = new Binding();
            binding.setVariable("t", token);
            setBinding(binding);

            return run();
        }
    }

    /**
     * Generates the link that forwards the user to the login page.
     */
    private URI initiateLogin(ContainerRequestContext request) {
        final Optional<IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
        if (idpConfigurationOpt.isEmpty()) {
            log.warn("Unable to initiate authentication, because idp configuration is not available.");
            return null;
        }
        JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();
        URI uri = UriBuilder.fromUri(idpConfiguration.getAuthorizationEndpoint())
                .queryParam("response_type","code")
                .queryParam("client_id", client)
                .queryParam("redirect_uri", UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(AdminServlet.ADMIN_UI).build())
                .queryParam("scope","openid")
                .queryParam("state", UUID.randomUUID()).build();
        return uri;
    }


    /**
     * Checks if the incoming request is an authentication callback.
     */
    @SneakyThrows
    private Response checkForAuthCallback(ContainerRequestContext request) {
        final Optional<IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
        if (idpConfigurationOpt.isEmpty()) {
            log.warn("Unable to start authentication, because idp configuration is not available.");
            return null;
        }
        JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();

        final String code = request.getUriInfo().getQueryParameters().getFirst("code");
        if (code == null) {
            return null;
        }

        // Build the original redirect uri
        final URI requestUri = UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(AdminServlet.ADMIN_UI).build();
        log.info("Request URI: {}", requestUri);
        final TokenRequest tokenRequest = new TokenRequest(
                UriBuilder.fromUri(idpConfiguration.getTokenEndpoint()).build(),
                new ClientID(client),
                new AuthorizationCodeGrant(new AuthorizationCode(code), requestUri)
        );

        TokenResponse response = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            HTTPResponse httpResponse = response.toHTTPResponse();
            log.warn("Unable to retrieve access token from auth server: {}", httpResponse.getContent());
            return null;
        }
        else if (!(response instanceof AccessTokenResponse)) {
            log.warn("Unknown token response {}.", response.getClass().getName());
            return null;
        }

        AccessTokenResponse successResponse = (AccessTokenResponse) response;

        // Get the access token, the server may also return a refresh token
        com.nimbusds.oauth2.sdk.token.AccessToken accessToken = successResponse.getTokens().getAccessToken();

        URI uri = request.getUriInfo().getRequestUriBuilder().replaceQuery("").build();

        return Response
                .seeOther(uri)
                .header(HttpHeaders.SET_COOKIE, authCookieCreator.apply(request,accessToken.getValue()))
                .build();
    }
}
