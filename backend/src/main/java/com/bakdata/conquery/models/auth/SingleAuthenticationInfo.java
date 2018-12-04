package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;

import lombok.Getter;

@Getter
public class SingleAuthenticationInfo implements AuthenticationInfo {
	
	private static final long serialVersionUID = -4533552484464540727L;
	
	private final  SinglePrincipalCollection principals;
	private final Object credentials;
	
	public SingleAuthenticationInfo(Object principal, Object credentials) {
		this.principals = new SinglePrincipalCollection((PermissionOwnerId<?>)principal);
		this.credentials = credentials;
	}
}
