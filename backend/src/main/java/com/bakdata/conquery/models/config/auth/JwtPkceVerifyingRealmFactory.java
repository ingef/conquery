package com.bakdata.conquery.models.config.auth;

import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.RequestHelper;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.oidc.JwtPkceVerifyingRealm;
import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jetbrains.annotations.Nullable;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.representations.AccessToken;

/**
 * A realm that verifies oauth tokens using PKCE.
 * <p>
 * Since the adminEnd-UI mainly works with direct links that do not support setting of the Authorization-header to transport an access token
 * and it also does not support the oauth code flow, this realm proxies the flow.
 * 1. The user/client is redirected to the IDP for authentication and redirected back to the adminEnd
 * 2. The this realm then picks up the code the transported authorization code from the query and redeems it for an access token and refresh token.
 * 3. Both are converted to cookies, which are saved on the client upon redirection to the page the user initially
 * wanted to visit.
 * 4. With the redirection the client sends back the cookies from which the adminEnd extracts the access token and verifies it.
 * (5.) If the previous step failed. The refresh token is extracted and exchanged for a fresh access token and refresh token and continues with step 3.
 */
@CPSType(id = "JWT_PKCE_REALM", base = AuthenticationRealmFactory.class)
@NoArgsConstructor
@Data
@Slf4j
public class JwtPkceVerifyingRealmFactory implements AuthenticationRealmFactory {

	public static final String REFRESH_TOKEN = "refresh_token";
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
	private volatile IdpConfiguration idpConfiguration;

	/**
	 * A leeway for token's expiration in seconds, this should be a short time.
	 * <p>
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

	/**
	 * @param signingKeys The public key information that is used to validate signed JWT.
	 *                    It can be retrieved from the IDP.
	 */
	public record IdpConfiguration(
			@NonNull Map<String, PublicKey> signingKeys,
			@NonNull URI authorizationEndpoint,
			@NonNull URI tokenEndpoint,
			@NonNull URI logoutEndpoint,
			@NotEmpty String issuer) {
	}

	public ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController) {
		final List<TokenVerifier.Predicate<AccessToken>> additionalVerifiers = new ArrayList<>();

		for (String additionalTokenCheck : additionalTokenChecks) {
			additionalVerifiers.add(ScriptedTokenChecker.create(additionalTokenCheck));
		}


		idpConfigurationSupplier = getIdpOptionsSupplier(environment, config);
		authCookieCreator = config.getAuthentication()::createAuthCookie;

		// Add login schema for admin UI
		final DropwizardResourceConfig jerseyAdminUi = authorizationController.getAdminServlet().getJerseyConfigUI();
		RedirectingAuthFilter.registerLoginInitiator(jerseyAdminUi, this::initiateLogin, "jwt-initiator");
		RedirectingAuthFilter.registerAuthAttemptChecker(jerseyAdminUi, this::checkAndRedeemAuthzCode, "jwt-authz-redeemer");
		RedirectingAuthFilter.registerAuthAttemptChecker(jerseyAdminUi, this::checkAndRedeemRefreshToken, "jwt-refresh-redeemer");

		return new JwtPkceVerifyingRealm(idpConfigurationSupplier,
										 client,
										 additionalVerifiers,
										 alternativeIdClaims,
										 authorizationController.getStorage(),
										 tokenLeeway,
										 environment.getValidator()
		);
	}

	@Data
	private static class JWKs {
		List<JWK> keys;
	}

	private Supplier<Optional<IdpConfiguration>> getIdpOptionsSupplier(Environment environment, ConqueryConfig config) {

		return () -> {
			if (idpConfiguration == null) {
				synchronized (this) {
					// check again since we are now in an exclusive section
					if (idpConfiguration == null) {

						final Client httpClient = new JerseyClientBuilder(environment).using(config.getJerseyClient())
																					  .build(this.getClass().getSimpleName());
						try {
							// retrieve the configuration and cache it
							idpConfiguration = retrieveIdpConfiguration(httpClient);
						}
						finally {
							httpClient.close();
						}
					}
				}
			}
			return Optional.ofNullable(idpConfiguration);
		};
	}

	/**
	 * Retrieve the configuration of the IDP from its "wellknown"-endpoint.
	 *
	 * @implNote Since this involves https requests, ensure to cache the returned value.
	 */
	public IdpConfiguration retrieveIdpConfiguration(final Client client) {

		if (wellKnownEndpoint == null) {
			log.error("Cannot retrieve idp configuration, because no well-known endpoint was given");
			return null;
		}

		JsonNode response;
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
		URI logoutEndpoint = URI.create(response.get("end_session_endpoint").asText());

		URI jwksUri = URI.create(response.get("jwks_uri").asText());

		JWKs jwks;
		try {
			jwks = client.target(jwksUri)
						 .request(MediaType.APPLICATION_JSON_TYPE)
						 .get(JWKs.class);

		}
		catch (Exception e) {
			log.warn("Unable to retrieve jwks from {}", wellKnownEndpoint, e);
			return null;
		}

		if (jwks == null) {
			return null;
		}


		// Filter for keys that are used for signing (discard encryption keys)
		final Map<String, PublicKey> signingKeys = jwks.getKeys().stream()
													   .filter(jwk -> JWK.Use.SIG.asString().equals(jwk.getPublicKeyUse()))
													   .collect(Collectors.toMap(JWK::getKeyId, JwtPkceVerifyingRealmFactory::getPublicKey));


		if (signingKeys.isEmpty()) {
			throw new IllegalStateException("No signing keys could be retrieved from IDP. Received these JWKs (Key Ids):" + jwks.getKeys()
																																.stream()
																																.map(JWK::getKeyId).toList());
		}

		return new IdpConfiguration(signingKeys, authorizationEndpoint, tokenEndpoint, logoutEndpoint, issuer);
	}


	@JsonIgnore
	@SneakyThrows(JsonProcessingException.class)
	private static PublicKey getPublicKey(JWK jwk) {
		// We have to re-serdes the object because it might be a sub class which can not be handled correctly by the JWKParser
		String jwkString = Jackson.MAPPER.writeValueAsString(jwk);
		return JWKParser.create().parse(jwkString).toPublicKey();
	}

	public abstract static class ScriptedTokenChecker extends Script implements TokenVerifier.Predicate<AccessToken> {

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
		return UriBuilder.fromUri(idpConfiguration.authorizationEndpoint())
						 .queryParam("response_type", "code")
						 .queryParam("client_id", client)
						 .queryParam("redirect_uri", UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(AdminServlet.ADMIN_UI).build())
						 .queryParam("scope", "openid")
						 .queryParam("state", UUID.randomUUID()).build();
	}


	/**
	 * Checks if the incoming request is an authentication callback.
	 */
	private Response checkAndRedeemAuthzCode(ContainerRequestContext request) {

		// Extract the authorization code
		final String code = request.getUriInfo().getQueryParameters().getFirst("code");
		if (code == null) {
			return null;
		}

		// Build the original redirect uri (the request uri without the query added by the IDP)
		final URI redirectedUri =
				UriBuilder.fromUri(RequestHelper.getRequestURL(request)).replacePath(request.getUriInfo().getAbsolutePath().getPath()).replaceQuery("").build();
		log.trace("Redirect URI: {}", redirectedUri);

		// Prepare code for exchange with access token
		final AuthorizationCodeGrant authzGrant = new AuthorizationCodeGrant(
				new AuthorizationCode(code),
				redirectedUri
		);

		// Redeem code
		AccessTokenResponse tokenResponse = getTokenResponse(authzGrant);
		if (tokenResponse == null) {
			return null;
		}

		// Get the access token, the server may also return a refresh token
		final Cookie accessTokenCookie = prepareAccessTokenCookie(request, tokenResponse);
		final NewCookie refreshTokenCookie = prepareRefreshTokenCookie(request, tokenResponse);

		// Let the client call the same uri again, but this time with valid credentials
		return prepareRedirectResponse(redirectedUri, accessTokenCookie, refreshTokenCookie);
	}


	/**
	 * Checks if the incoming request has a valid refresh token.
	 */
	private Response checkAndRedeemRefreshToken(ContainerRequestContext request) {

		// Extract the refresh token which was previously saved in a cookie
		final Cookie refreshToken = request.getCookies().get(REFRESH_TOKEN);

		if (refreshToken == null) {
			return null;
		}

		// Redeem refresh token for a new access token (+  new refresh token)
		final AccessTokenResponse tokenResponse = getTokenResponse(new RefreshTokenGrant(new RefreshToken(refreshToken.getValue())));
		if (tokenResponse == null) {
			return null;
		}

		// Prepare separate cookies for access token an refresh token
		final Cookie accessTokenCookie = prepareAccessTokenCookie(request, tokenResponse);
		final NewCookie refreshTokenCookie = prepareRefreshTokenCookie(request, tokenResponse);

		return prepareRedirectResponse(request.getUriInfo().getRequestUriBuilder().replaceQuery("").build(), accessTokenCookie, refreshTokenCookie);
	}

	/**
	 * Prepares a redirect response which also saves the access and refresh token on the client.
	 */
	private Response prepareRedirectResponse(URI uri, Cookie accessTokenCookie, NewCookie refreshTokenCookie) {
		return Response
				.seeOther(uri)
				.header(HttpHeaders.SET_COOKIE, accessTokenCookie)
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
				.build();
	}

	private Cookie prepareAccessTokenCookie(ContainerRequestContext request, AccessTokenResponse tokenResponse) {
		com.nimbusds.oauth2.sdk.token.AccessToken accessToken = tokenResponse.getTokens().getAccessToken();
		return authCookieCreator.apply(request, accessToken.getValue());
	}

	/**
	 * Extracts a refresh token and converts it into a cookies with the life span of that refresh token.
	 */
	@SneakyThrows(java.text.ParseException.class)
	private NewCookie prepareRefreshTokenCookie(ContainerRequestContext request, AccessTokenResponse tokenResponse) {
		RefreshToken refreshToken = tokenResponse.getTokens().getRefreshToken();

		final Date exp = (Date) JWTParser.parse(refreshToken.getValue()).getJWTClaimsSet().getClaim("exp");
		if (exp == null) {
			throw new IllegalStateException("Refresh token did not include an expiration");
		}


		return new NewCookie(
				REFRESH_TOKEN,
				refreshToken.getValue(),
				"/",
				null,
				0,
				null,
				900, //The Constructor requires us to set a maxAge even though it should be optional. 30 minutes should be okay
				exp,
				request.getSecurityContext().isSecure(),
				true
		);
	}

	/**
	 * Tries to redeem the {@link AuthorizationGrant} for an {@link com.nimbusds.oauth2.sdk.token.AccessToken} ( + {@link RefreshToken}).
	 */
	@Nullable
	@SneakyThrows({ParseException.class, IOException.class})
	private AccessTokenResponse getTokenResponse(AuthorizationGrant authzGrant) {

		// Retrieve the IDP configuration
		final Optional<IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
		if (idpConfigurationOpt.isEmpty()) {
			log.warn("Unable to start authentication, because idp configuration is not available.");
			return null;
		}
		JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();

		// Send the auth code/refresh token to the IDP to redeem them for a new access and refresh token
		final TokenRequest tokenRequest = new TokenRequest(
				UriBuilder.fromUri(idpConfiguration.tokenEndpoint()).build(),
				new ClientID(client),
				authzGrant
		);

		// Get the response
		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		TokenResponse response = TokenResponse.parse(httpRequest.send());

		// Check if the response was valid
		if (!response.indicatesSuccess()) {
			HTTPResponse httpResponse = response.toHTTPResponse();
			log.warn("Unable to retrieve access token from auth server. Request: {} Response: {}", httpRequest.getURL(), httpResponse.getBody());
			return null;
		}
		else if (!(response instanceof AccessTokenResponse)) {
			log.warn("Unknown token response {}.", response.getClass().getName());
			return null;
		}

		return (AccessTokenResponse) response;
	}
}
