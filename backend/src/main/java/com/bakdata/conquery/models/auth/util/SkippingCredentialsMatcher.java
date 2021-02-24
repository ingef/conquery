package com.bakdata.conquery.models.auth.util;

import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

/**
 * Noop class for the {@link CredentialsMatcher} in case the provided token already contains the {@link AuthenticationInfo}.
 * E.g. the JWT in the {@link LocalAuthenticationRealm}.
 */
public enum SkippingCredentialsMatcher implements CredentialsMatcher {
	INSTANCE();

	@Override
	public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
		return true;
	}
	
}