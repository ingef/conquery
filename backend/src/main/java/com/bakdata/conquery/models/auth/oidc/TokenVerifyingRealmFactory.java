package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.commands.ManagerNode;
import org.apache.shiro.realm.Realm;
import org.keycloak.authorization.client.Configuration;

public class TokenVerifyingRealmFactory extends Configuration {

    Realm createRealm(ManagerNode manager) {
        return null;
    }
}
