package com.bakdata.conquery.models.auth.util;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;

import lombok.Getter;

/**
 * Specific implementation of AuthenticationInfo that uses the
 * {@link SinglePrincipalCollection} to pack principals.
 *
 */
@Getter
public class SingleAuthenticationInfo implements AuthenticationInfo {

	private static final long serialVersionUID = -4533552484464540727L;

	private final SinglePrincipalCollection principals;
	private final Object credentials;

	public SingleAuthenticationInfo(Object principal, Object credentials) {
		this.principals = new SinglePrincipalCollection((PermissionOwnerId<?>) principal);
		this.credentials = credentials;
	}
}
