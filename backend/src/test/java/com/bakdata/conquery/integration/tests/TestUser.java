package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import org.apache.shiro.authz.Permission;

import java.util.Collection;
import java.util.List;

public class TestUser extends User {

    public TestUser(MetaStorage storage) {
        super("user", "user", storage::updateUser);
    }

    public boolean isPermitted(Permission permission) {
        return getShiroUserAdapter().isPermitted(permission);
    }

    public boolean[] isPermitted(List<Permission> permissions) {
        return getShiroUserAdapter().isPermitted(permissions);
    }

    public boolean isPermittedAll(Collection<Permission> permissions) {
        return getShiroUserAdapter().isPermittedAll(permissions);
    }
}
