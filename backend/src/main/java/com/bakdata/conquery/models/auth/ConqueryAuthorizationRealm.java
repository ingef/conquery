package com.bakdata.conquery.models.auth;

import java.util.HashSet;
import java.util.Objects;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

@RequiredArgsConstructor
public class ConqueryAuthorizationRealm extends AuthorizingRealm {
	
	public final AuthorizationStorage storage;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Objects.requireNonNull(principals, "No principal info was provided");
		UserId userId = UserId.class.cast(principals.getPrimaryPrincipal());
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		
		info.addObjectPermissions(new HashSet<Permission>(AuthorizationHelper.getEffectiveUserPermissions(userId, storage)));
		
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		// This Realm only authorizes
		return null;
	}

}
