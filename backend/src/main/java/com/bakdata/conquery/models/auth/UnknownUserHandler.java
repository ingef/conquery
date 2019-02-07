package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

/**
 * A handler that takes care of requests that have credentials, that could be
 * parsed but are not known to the system.
 *
 */
public interface UnknownUserHandler {

	/**
	 * Handles valid user information unknown to the system.
	 * 
	 * @param info Info about the unknown user.
	 * @return A new {@link User} constructed from the {@link AuthenticationInfo},
	 * a default user, or {@code null} if the request should fail.
	 */
	User handle(AuthenticationInfo info);
}
