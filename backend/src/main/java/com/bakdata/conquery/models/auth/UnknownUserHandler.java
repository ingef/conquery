package com.bakdata.conquery.models.auth;

import java.util.Optional;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

public interface UnknownUserHandler {
	Optional<User> handle(AuthenticationInfo info);
}
