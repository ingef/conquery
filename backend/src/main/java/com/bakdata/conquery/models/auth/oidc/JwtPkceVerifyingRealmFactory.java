package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.shiro.realm.Realm;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;

import java.security.PublicKey;

/**
 * A realm that verifies oauth tokens using PKCE.
 */
@CPSType(id = "JWT_PKCE_REALM", base = AuthenticationConfig.class)
@NoArgsConstructor
@Data
public class JwtPkceVerifyingRealmFactory implements AuthenticationConfig {

    private JWK jwk;

    public ConqueryAuthenticationRealm createRealm(ManagerNode manager) {
        return new JwtPkceVerifyingRealm(getPublicKey(jwk));
    }


    @JsonIgnore
    @SneakyThrows(JsonProcessingException.class)
    private static PublicKey getPublicKey(JWK jwk) {
        // We have to re-serdes the object because it might be a sub class which can not be handled correctly by the JWKParser
        String jwkString = Jackson.MAPPER.writeValueAsString(jwk);
        return JWKParser.create().parse(jwkString).toPublicKey();
    }
}
