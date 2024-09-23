package com.bakdata.conquery.models.auth.oidc;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.validation.Validator;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.JOSEParser;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;

/**
 * This realm uses the configured public key to verify the signature of a provided JWT and extracts informations about
 * the authenticated user from it.
 */
@Slf4j
@Data
public class JwtPkceVerifyingRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;
	//TODO FK/MT: Investigate difference between current allowedAudience impl and supposed audience (without mapper).
	private final String[] allowedAudience;
	private final TokenVerifier.Predicate<JsonWebToken>[] tokenChecks;
	private final List<String> alternativeIdClaims;
	private final ActiveWithLeewayVerifier activeVerifier;
	private final MetaStorage storage;
	private final Validator validator;

	/**
	 * Used in handleRoleClaims as size-limited set, with LRU characteristics.
	 * @implNote maximumSize is an arbitrary medium high number to avoid stuffing memory with token hashes, while avoiding reprocessing known access tokens.
	 */
	private final Cache<String, String> processedRoleClaims = CacheBuilder.newBuilder()
																		  .maximumSize(1_000)
																		  .build();

	Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier;


	public JwtPkceVerifyingRealm(
			@NonNull Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier,
			@NonNull String allowedAudience,
			List<TokenVerifier.Predicate<AccessToken>> additionalTokenChecks,
			List<String> alternativeIdClaims,
			MetaStorage storage,
			int tokenLeeway,
			Validator validator
	) {
		this.storage = storage;
		this.idpConfigurationSupplier = idpConfigurationSupplier;
		this.allowedAudience = new String[]{allowedAudience};
		this.alternativeIdClaims = alternativeIdClaims;
		this.tokenChecks = additionalTokenChecks.toArray(TokenVerifier.Predicate[]::new);
		this.validator = validator;
		setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		setAuthenticationTokenClass(TOKEN_CLASS);
		activeVerifier = new ActiveWithLeewayVerifier(tokenLeeway);
	}

	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

		final Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();

		if (idpConfigurationOpt.isEmpty()) {
			log.warn("Unable to start authentication, because idp configuration is not available.");
			return null;
		}

		final JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();
		final BearerToken bearerToken = (BearerToken) token;

		log.trace("Parsing token ({}) to extract key id from header", bearerToken.getToken());

		final String keyId = JOSEParser.parse(bearerToken.getToken()).getHeader().getKeyId();

		log.trace("Key id of token signer: {}", keyId);

		final PublicKey publicKey = idpConfiguration.signingKeys().get(keyId);

		if (publicKey == null) {
			throw new UnsupportedTokenException("Token was signed by a key with an unknown Id: " + keyId);
		}

		log.trace("Creating token verifier");
		final TokenVerifier<AccessToken>
				verifier =
				TokenVerifier.create(bearerToken.getToken(), AccessToken.class)
							 .withChecks(new TokenVerifier.RealmUrlCheck(idpConfiguration.issuer()), TokenVerifier.SUBJECT_EXISTS_CHECK, activeVerifier)
							 .withChecks(tokenChecks)
							 .publicKey(publicKey)
							 .audience(allowedAudience);

		log.trace("Verifying token");

		final AccessToken accessToken;
		try {
			verifier.verify();
			accessToken = verifier.getToken();
		}
		catch (VerificationException e) {
			log.trace("Verification failed", e);
			throw new IncorrectCredentialsException(e);
		}

		final String subject = accessToken.getSubject();

		if (subject == null) {
			// Should not happen, as sub is mandatory in an access_token
			throw new UnsupportedTokenException("Unable to extract a subject from the provided token.");
		}

		log.trace("Authentication was successful for subject: {}", subject);


		UserId userId = new UserId(subject);
		User user = storage.getUser(userId);

		if (user != null) {
			log.trace("Successfully authenticated user {}", userId);
			handleRoleClaims(accessToken, user);
			return new ConqueryAuthenticationInfo(user, token, this, true, idpConfiguration.logoutEndpoint());
		}


		// Try alternative ids
		for (String alternativeIdClaim : alternativeIdClaims) {

			final Object altId = accessToken.getOtherClaims().get(alternativeIdClaim);

			if (!(altId instanceof String)) {
				log.trace("Found no value for alternative id claim {}", alternativeIdClaim);
				continue;
			}

			userId = new UserId((String) altId);
			user = storage.getUser(userId);

			if (user != null) {
				log.trace("Successfully mapped subject {} using user id {}", accessToken.getSubject(), userId);
				handleRoleClaims(accessToken, user);
				return new ConqueryAuthenticationInfo(user, token, this, true, idpConfiguration.logoutEndpoint());
			}
		}

		// Create a new user if none could be found
		final User newUser = createUser(accessToken);

		return new ConqueryAuthenticationInfo(newUser, token, this, true, idpConfiguration.logoutEndpoint());
	}

	/**
	 * Creates a new user from values in the access token
	 */
	private User createUser(AccessToken accessToken) {
		String userLabel = ObjectUtils.firstNonNull(accessToken.getName(), accessToken.getPreferredUsername(), accessToken.getSubject());

		final User user = new User(accessToken.getSubject(), userLabel, storage);

		ValidatorHelper.failOnError(log, getValidator().validate(user));

		user.updateStorage();
		handleRoleClaims(accessToken, user);

		log.info("Created a new user from a valid JWT: {}", user);

		return user;
	}

	private void handleRoleClaims(AccessToken accessToken, User user) {

		if (processedRoleClaims.getIfPresent(accessToken.getId()) != null) {
			log.trace("Already handled role claims of {}", accessToken.getId());
			return;
		}

		processedRoleClaims.put(accessToken.getId(), accessToken.getId());

		//TODO handle removal of role claim? (probably not!?)

		final Map<String, AccessToken.Access> resourceAccess = accessToken.getResourceAccess();

		if (resourceAccess == null) {
			log.trace("No resource Access present.");
			return;
		}

		final AccessToken.Access access = resourceAccess.get(getAllowedAudience()[0]);

		if (access == null) {
			log.trace("No resource access found for {}.", getAllowedAudience()[0]);
			return;
		}

		final Set<String> roleClaims = access.getRoles();

		if (roleClaims == null) {
			log.trace("No role claims found.");
			return;
		}

		log.trace("Found role claims for {}: {}.", user, roleClaims);


		for (String roleClaim : roleClaims) {
			final RoleId roleId = new RoleId(roleClaim);

			if (user.getRoles().contains(roleId)) {
				log.trace("Role {} already registered.", roleId);
				continue;
			}

			final Role role = storage.getRole(roleId);

			if (role == null) {
				continue;
			}

			log.trace("Adding {} to {}", role, user);

			user.addRole(role);
		}
	}

}
