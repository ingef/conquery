package com.bakdata.conquery.models.auth;

import java.util.Optional;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

public class DefaultUnknownUserHandler implements UnknownUserHandler {

	@Override
	public Optional<User> handle(AuthenticationInfo info) {
		return Optional.empty();
	}

}
