package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.subjects.User;

public interface UnknownUserHandler {
	User handle(AuthenticationInfo info);
}
