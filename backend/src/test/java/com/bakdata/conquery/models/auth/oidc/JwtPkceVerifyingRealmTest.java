package com.bakdata.conquery.models.auth.oidc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.authc.BearerToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.keycloak.common.VerificationException;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtPkceVerifyingRealmTest {


    private static final MetaStorage STORAGE =  new NonPersistentStoreFactory().createMetaStorage();
    private static final String HTTP_REALM_URL = "http://realm.url";
    private static final String AUDIENCE = "test_aud";
    private static final String ALTERNATIVE_ID_CLAIM = "alternativeId";
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
        REALM = new JwtPkceVerifyingRealm(
                () -> Optional.of(new JwtPkceVerifyingRealmFactory.IdpConfiguration( PUBLIC_KEY, URI.create("auth"), URI.create("token"), HTTP_REALM_URL)),
                AUDIENCE,
                List.of(JwtPkceVerifyingRealmFactory.ScriptedTokenChecker.create("t.getOtherClaims().get(\"groups\").equals(\"conquery\")")),
                List.of(ALTERNATIVE_ID_CLAIM),
                STORAGE,
                60);
    }


    @Test
    void verifyToken() {

        // Setup the expected user id
        User expected = new User("Test", "Test", STORAGE);
        STORAGE.updateUser(expected);

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withAudience(AUDIENCE)
                .withSubject(expected.getName())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThat(REALM.doGetAuthenticationInfo(accessToken).getPrincipals().getPrimaryPrincipal()).isEqualTo(expected);
    }

    @Test
    void verifyTokenInLeeway() {

        // Setup the expected user id
		User expected = new User("Test", "Test", STORAGE);

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, -1);
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withAudience(AUDIENCE)
                .withSubject(expected.getName())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThat(REALM.doGetAuthenticationInfo(accessToken).getPrincipals().getPrimaryPrincipal()).isEqualTo(expected);
    }


    @Test
    void verifyTokenAlternativeId() {

        // Setup the expected user id
        User expected = new User("Test", "Test", STORAGE);
        STORAGE.updateUser(expected);

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        final String primId = UUID.randomUUID().toString();
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withAudience(AUDIENCE)
                .withSubject(primId)
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .withClaim(ALTERNATIVE_ID_CLAIM, expected.getName())
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThat(REALM.doGetAuthenticationInfo(accessToken).getPrincipals().getPrimaryPrincipal()).isEqualTo(expected);
    }


    @Test
    void falsifyTokenMissingCustomClaim() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withAudience(AUDIENCE)
                .withSubject(expected.getName())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }

    @Test
    void falsifyTokenWrongAudience() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withAudience("wrong_aud")
                .withSubject(expected.getName())
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }

    @Test
    void falsifyTokenOutdated() {
        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, -2);
        String token = JWT.create()
                .withIssuer(HTTP_REALM_URL)
                .withSubject(expected.getName())
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }

    @Test
    void falsifyTokenWrongIssuer() {

        // Setup the expected user id
        UserId expected = new UserId("Test");

        Date issueDate = new Date();
        Date expDate = DateUtils.addMinutes(issueDate, 1);
        String token = JWT.create()
                .withIssuer("wrong_iss")
                .withAudience(AUDIENCE)
                .withSubject(expected.getName())
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .withClaim("groups", "conquery")
                .withIssuedAt(issueDate)
                .withExpiresAt(expDate)
                .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY));
        BearerToken accessToken = new BearerToken(token);

        assertThatCode(() -> REALM.doGetAuthenticationInfo(accessToken)).hasCauseInstanceOf(VerificationException.class);
    }
}