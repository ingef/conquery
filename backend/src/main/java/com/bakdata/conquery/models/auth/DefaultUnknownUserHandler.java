package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

public class DefaultUnknownUserHandler implements UnknownUserHandler {

	@Override
	public User handle(AuthenticationInfo info) {
		return null;
	}

}
