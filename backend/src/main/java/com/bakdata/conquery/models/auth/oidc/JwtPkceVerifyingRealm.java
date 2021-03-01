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

import java.security.PublicKey;


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

        verifier.publicKey(publicKey);
        verifier
                .withChecks(t -> t.isActive());

        String subject;
        log.trace("Verifying token");
        try {
            verifier.verify();
            subject = verifier.getToken().getSubject();
        } catch (VerificationException e) {
            log.trace("Verification failed",e);
            throw new IncorrectCredentialsException(e);
        }

        if (subject == null) {
            log.trace("Could not find a subject in provided token.");
            throw new UnsupportedTokenException("Unable to extract a subject from the provided token.");
        }


        return new ConqueryAuthenticationInfo(new UserId(subject), token, this, true);
    }

}
