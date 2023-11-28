package com.bakdata.conquery.models.auth.oidc;

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
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
public class JwtPkceVerifyingRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;

	Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier;
	private final String[] allowedAudience;
	private final TokenVerifier.Predicate<JsonWebToken>[] tokenChecks;
	private final List<String> alternativeIdClaims;
	private final ActiveWithLeewayVerifier activeVerifier;
	private final MetaStorage storage;

	public JwtPkceVerifyingRealm(@NonNull Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier,
								 @NonNull String allowedAudience,
								 List<TokenVerifier.Predicate<AccessToken>> additionalTokenChecks,
								 List<String> alternativeIdClaims,
								 MetaStorage storage,
								 int tokenLeeway) {
		this.storage = storage;
		this.idpConfigurationSupplier = idpConfigurationSupplier;
		this.allowedAudience = new String[]{allowedAudience};
		this.tokenChecks = additionalTokenChecks.toArray((TokenVerifier.Predicate<JsonWebToken>[]) Array.newInstance(TokenVerifier.Predicate.class, 0));
		this.alternativeIdClaims = alternativeIdClaims;
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.activeVerifier = new ActiveWithLeewayVerifier(tokenLeeway);
	}


	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
		if (idpConfigurationOpt.isEmpty()) {
			log.warn("Unable to start authentication, because idp configuration is not available.");
			return null;
		}
		JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();
		final BearerToken bearerToken = (BearerToken) token;

		log.trace("Parsing token ({}) to extract key id from header", bearerToken.getToken());
		final String keyId = JOSEParser.parse(bearerToken.getToken()).getHeader().getKeyId();
		log.trace("Key id of token signer: {}", keyId);
		final PublicKey publicKey = idpConfiguration.signingKeys().get(keyId);

		if (publicKey == null) {
			throw new UnsupportedTokenException("Token was signed by a key with an unknown Id: " + keyId);
		}

		log.trace("Creating token verifier");
		TokenVerifier<AccessToken> verifier = TokenVerifier.create(bearerToken.getToken(), AccessToken.class)
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
			return new ConqueryAuthenticationInfo(user, token, this, true);
		}

		// Try alternative ids
		for (String alternativeIdClaim : alternativeIdClaims) {
			Object altId = accessToken.getOtherClaims().get(alternativeIdClaim);
			if (!(altId instanceof String)) {
				log.trace("Found no value for alternative id claim {}", alternativeIdClaim);
				continue;
			}
			userId = new UserId((String) altId);
			user = storage.getUser(userId);
			if (user != null) {
				log.trace("Successfully mapped subject {} using user id {}", subject, userId);
				return new ConqueryAuthenticationInfo(user, token, this, true);
			}
		}

		throw new UnknownAccountException("The user id was unknown: " + subject);
	}

}
