package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.TokenNotActiveException;
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
public class JwtPkceVerifyingRealm extends ConqueryAuthenticationRealm {

    private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;

    Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier;
    private final String[] allowedAudience;
    private final TokenVerifier.Predicate<JsonWebToken>[] tokenChecks;
    private final List<String> alternativeIdClaims;
    private final ActiveWithLeewayVerifier activeVerifier;

    public JwtPkceVerifyingRealm(@NonNull Supplier<Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration>> idpConfigurationSupplier, @NonNull String allowedAudience, List<TokenVerifier.Predicate<AccessToken>> additionalTokenChecks, List<String> alternativeIdClaims, int tokenLeeway) {
        this.idpConfigurationSupplier = idpConfigurationSupplier;
        this.allowedAudience = new String[] {allowedAudience};
        this.tokenChecks = additionalTokenChecks.toArray((TokenVerifier.Predicate<JsonWebToken>[])Array.newInstance(TokenVerifier.Predicate.class,0));
        this.alternativeIdClaims = alternativeIdClaims;
        this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
        this.setAuthenticationTokenClass(TOKEN_CLASS);
        this.activeVerifier = new ActiveWithLeewayVerifier(tokenLeeway);
    }


    @Override
    protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        Optional<JwtPkceVerifyingRealmFactory.IdpConfiguration> idpConfigurationOpt = idpConfigurationSupplier.get();
        if (idpConfigurationOpt.isEmpty()) {
            log.warn("Unable to start authentication, because idp configuration is not available.");
            return null;
        }
        JwtPkceVerifyingRealmFactory.IdpConfiguration idpConfiguration = idpConfigurationOpt.get();

        log.trace("Creating token verifier");
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(((BearerToken) token).getToken(), AccessToken.class)
                .withChecks(new TokenVerifier.RealmUrlCheck(idpConfiguration.getIssuer()), TokenVerifier.SUBJECT_EXISTS_CHECK, activeVerifier)
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

        // Extract alternative ids
        List<UserId> alternativeIds = new ArrayList<>();
        for (String alternativeIdClaim : alternativeIdClaims) {
            Object altId = accessToken.getOtherClaims().get(alternativeIdClaim);
            if (!(altId instanceof String)) {
                log.trace("Found no value for alternative id claim {}", alternativeIdClaim);
                continue;
            }
            alternativeIds.add(new UserId((String) altId));
        }

        return new ConqueryAuthenticationInfo(new UserId(subject), token, this, true, alternativeIds);
    }

    public static final TokenVerifier.Predicate<JsonWebToken> IS_ACTIVE = new TokenVerifier.Predicate<JsonWebToken>() {
        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (! t.isActive()) {
                throw new TokenNotActiveException(t, "Token is not active");
            }

            return true;
        }
    };

}
