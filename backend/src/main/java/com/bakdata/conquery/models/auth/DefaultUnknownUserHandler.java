package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

/**
 * Default dummy implementation for the UnknownUserHandler.
 *
 */
public class DefaultUnknownUserHandler implements UnknownUserHandler {

	@Override
	public User handle(AuthenticationInfo info) {
		return null;
	}

}
