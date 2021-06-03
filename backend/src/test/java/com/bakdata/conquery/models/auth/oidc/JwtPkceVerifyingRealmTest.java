package com.bakdata.conquery.models.auth.oidc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.authc.BearerToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtPkceVerifyingRealmTest {

    private static String AUDIENCE = "test_aud";
    private static JwtPkceVerifyingRealm REALM;
    private static RSAPrivateKey PRIVATE_KEY;
    private static RSAPublicKey PUBLIC_KEY;

    @BeforeAll
    static void setup() throws NoSuchAlgorithmException {
        // Generate a key pair
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024);
        KeyPair pair = keyGenerator.genKeyPair();
        PRIVATE_KEY = (RSAPrivateKey) pair.getPrivate();
        PUBLIC_KEY = (RSAPublicKey) pair.getPublic();

        // Create the realm
        REALM = new JwtPkceVerifyingRealm(PUBLIC_KEY, new String[] {AUDIENCE}, new TokenVerifier.Predicate[0]);
    }


    @Test
    void verifyValidToken() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer(REALM.getName())
                .withAudience(AUDIENCE)
                .withSubject(expected.getEmail())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThat(REALM.doGetConqueryAuthenticationInfo(accessToken).getPrincipals().getPrimaryPrincipal()).isEqualTo(expected);
    }

    @Test
    void verifyInvalidTokenAudience() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer(REALM.getName())
                .withAudience("wrong_aud")
                .withSubject(expected.getEmail())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetConqueryAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }

    @Test
    void verifyInvalidTokenOutdated() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, -1);
        String token = JWT.create()
                .withIssuer(REALM.getName())
                .withSubject(expected.getEmail())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetConqueryAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }
}