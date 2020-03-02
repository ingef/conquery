package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Getter;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * Specialization class of the {@link AuthenticationInfo} that enforces the use
 * of a {@link UserId} as primary principal.
 */
@SuppressWarnings("serial")
@Getter
public class ConqueryAuthenticationInfo implements AuthenticationInfo {

	private final SimplePrincipalCollection principals = new SimplePrincipalCollection();
	
	/**
	 * The credential a realm used for authentication.
	 */
	private final Object credentials;

	public ConqueryAuthenticationInfo(UserId userId, Object credentials, Realm realm) {
		principals.add(userId, realm.getName());
		this.credentials = credentials;
	}
}
