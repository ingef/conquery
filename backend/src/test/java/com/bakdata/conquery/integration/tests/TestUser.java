package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.models.auth.entities.User;
import org.apache.shiro.authz.Permission;

import java.util.Collection;
import java.util.List;

public class TestUser extends User {

    public TestUser() {
        super("user", "user");
    }

    public boolean isPermitted(Permission permission) {
        return shiroUserAdapter.isPermitted(permission);
    }

    public boolean[] isPermitted(List<Permission> permissions) {
        return shiroUserAdapter.isPermitted(permissions);
    }

    public boolean isPermittedAll(Collection<Permission> permissions) {
        return shiroUserAdapter.isPermittedAll(permissions);
    }
}
