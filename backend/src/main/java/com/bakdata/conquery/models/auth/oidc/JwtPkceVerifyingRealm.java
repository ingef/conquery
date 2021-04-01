package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * This realm uses the configured public key to verify the signature of a provided JWT and extracts informations about
 * the authenticated user from it.
 */
@Slf4j
public class JwtPkceVerifyingRealm extends ConqueryAuthenticationRealm {

    private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;

    private PublicKey publicKey;

    public JwtPkceVerifyingRealm(PublicKey publicKey) {

        this.publicKey = publicKey;
        this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
        this.setAuthenticationTokenClass(TOKEN_CLASS);
    }


    @Override
    protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        log.trace("Creating token verifier");
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(((BearerToken) token).getToken(), AccessToken.class);

        verifier = verifier.publicKey(publicKey)
                .withChecks(JsonWebToken::isActive);

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
            // Should not happen, as sub is mandatory in a access_token
            throw new UnsupportedTokenException("Unable to extract a subject from the provided token.");
        }

        // Extract alternative ids
        // TODO make configurable which claims might be mapped to a user id in the future
        List<UserId> alternativeIds = new ArrayList<>();
        String email = accessToken.getEmail();
        if(email != null) {
            alternativeIds.add(new UserId(email));
        }


        return new ConqueryAuthenticationInfo(new UserId(subject), token, this, true,alternativeIds);
    }

}
