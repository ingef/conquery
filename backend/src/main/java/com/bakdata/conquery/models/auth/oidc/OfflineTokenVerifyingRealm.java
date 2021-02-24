package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import java.security.PublicKey;

public class OfflineTokenVerifyingRealm extends ConqueryAuthenticationRealm {

    private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;
    private String publicKeyClass;
    private String publicKeySerialized;

    private PublicKey publicKey;

    @Override
    protected void onInit() {
        super.onInit();
        this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
        this.setAuthenticationTokenClass(TOKEN_CLASS);

        this.publicKey = parsePublicKey(publicKeySerialized,publicKeyClass);
    }

    @Override
    protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(((BearerToken) token).getToken(), AccessToken.class);

        verifier.publicKey(publicKey);
        verifier
                .withChecks(t -> t.isActive());

        String subject;
        try {
            verifier.verify();
            subject = verifier.getToken().getSubject();
        } catch (VerificationException e) {
            throw new IncorrectCredentialsException(e);
        }

        if (subject == null) {
            throw new UnsupportedTokenException("Unable to extract a subject from the provided token.");
        }


        return new ConqueryAuthenticationInfo(new UserId(subject), token, this, true);
    }

    private PublicKey parsePublicKey(String publicKeySerialized, String publicKeyClass) {
        try {
            return (PublicKey) Jackson.MAPPER.readValue(publicKeySerialized,Class.forName(publicKeyClass));
        } catch (JsonProcessingException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException("Unable to parse public key", e);
        }
    }
}
