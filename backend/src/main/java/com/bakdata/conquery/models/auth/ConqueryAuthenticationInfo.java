package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Getter;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;

@SuppressWarnings("serial")
@Getter
public class ConqueryAuthenticationInfo implements AuthenticationInfo {
	
	private final SimplePrincipalCollection principals = new SimplePrincipalCollection();
	private final Object credentials;
	
	public ConqueryAuthenticationInfo(UserId userId, Object credentials, Realm realm) {
		principals.add(userId, realm.getName());
		this.credentials = credentials;
	}
}
