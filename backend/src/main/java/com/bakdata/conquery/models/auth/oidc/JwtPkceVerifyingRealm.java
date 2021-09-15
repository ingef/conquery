package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
    private final MetaStorage storage;

    public JwtPkceVerifyingRealm(@NonNull Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier,
                                 @NonNull String allowedAudience,
                                 List<TokenVerifier.Predicate<AccessToken>> additionalTokenChecks,
                                 List<String> alternativeIdClaims,
                                 MetaStorage storage) {
        this.storage = storage;
        this.idpConfigurationSupplier = idpConfigurationSupplier;
        this.allowedAudience = new String[] {allowedAudience};
        this.tokenChecks = additionalTokenChecks.toArray((TokenVerifier.Predicate<JsonWebToken>[])Array.newInstance(TokenVerifier.Predicate.class,0));
        this.alternativeIdClaims = alternativeIdClaims;
        this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
        this.setAuthenticationTokenClass(TOKEN_CLASS);
    }


    @Override
    public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
        if (idpConfigurationOpt.isEmpty()) {
            log.warn("Unable to start authentication, because idp configuration is not available.");
            return null;
        }
        JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();

        log.trace("Creating token verifier");
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(((BearerToken) token).getToken(), AccessToken.class)
                .withChecks(new TokenVerifier.RealmUrlCheck(idpConfiguration.getIssuer()), TokenVerifier.SUBJECT_EXISTS_CHECK, TokenVerifier.IS_ACTIVE)
                .withChecks(tokenChecks)
                .publicKey(idpConfiguration.getPublicKey())
                .audience(allowedAudience);

        String subject;
        log.trace("Verifying token");
        AccessToken accessToken = null;
        try {
            verifier.verify();
            accessToken = verifier.getToken();
        } catch (VerificationException e) {
            log.trace("Verification failed",e);
            throw new IncorrectCredentialsException(e);
        }
        subject = accessToken.getSubject();

        if (subject == null) {
            // Should not happen, as sub is mandatory in an access_token
            throw new UnsupportedTokenException("Unable to extract a subject from the provided token.");
        }

        log.trace("Authentication successfull for subject {}", subject);


        UserId userId = new UserId(subject);
        User user = storage.getUser(userId);
        if (user != null) {
            log.trace("Successfully authenticated user {}", userId);
            return new ConqueryAuthenticationInfo(user, token, this, true);
        }

        // Try alternative ids
        List<UserId> alternativeIds = new ArrayList<>();
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
