package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;

import java.util.Collection;

/**
 * Specialization class of the {@link AuthenticationInfo} that enforces the use
 * of a {@link UserId} as primary principal.
 */
@SuppressWarnings("serial")
@Getter
@FieldNameConstants
@EqualsAndHashCode
public class ConqueryAuthenticationInfo implements AuthenticationInfo {

	private final SimplePrincipalCollection principals = new SimplePrincipalCollection();
	
	/**
	 * The credential a realm used for authentication.
	 */
	private final Object credentials;
	
	
	/**
	 * A realm can indicate whether a logout button is shown for the user or not
	 */
	private final boolean displayLogout; 

	public ConqueryAuthenticationInfo(UserId userId, Object credentials, Realm realm, boolean displayLogout) {
		this.credentials = credentials;
		this.displayLogout = displayLogout;
		principals.add(userId, realm.getName());
	}

	public ConqueryAuthenticationInfo(UserId userId, Object credentials, Realm realm, boolean displayLogout, Collection<UserId> alternativeIds) {
		this(userId, credentials, realm, displayLogout);
		principals.addAll(alternativeIds, realm.getName());
	}
}
