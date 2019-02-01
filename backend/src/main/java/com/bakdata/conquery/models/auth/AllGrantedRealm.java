package com.bakdata.conquery.models.auth;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.bakdata.conquery.models.auth.util.SingleAuthenticationInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * This realm authenticates and authorizes all requests given to it positive.
 */
@Slf4j
public class AllGrantedRealm extends AuthorizingRealm {
	/**
	 * The warning that is displayed, when the realm is instantiated.
	 */
	private static final String WARNING = "\n" +
			"           §§\n" +
			"          §  §\n" +
			"         §    §\n" +
			"        §      §\n" +
			"       §  §§§§  §       You instantiated and are probably using a Shiro realm\n" +
			"      §   §§§§   §      that does not do any permission checks or authentication.\n" +
			"     §     §§     §     Access to all resources is granted to everyone.\n" +
			"    §      §§      §    DO NOT USE THIS REALM IN PRODUCTION\n" +
			"   $                §\n" +
			"  §        §§        §\n" +
			" §                    §\n" +
			" §§§§§§§§§§§§§§§§§§§§§§";
	
	/**
	 * Standard constructor.
	 */
	public AllGrantedRealm() {
		log.warn(WARNING);
		this.setAuthenticationTokenClass(ConqueryToken.class);
		this.setCredentialsMatcher(new AllGrantedCredentialsMatcher());
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(new AllGrantedPermission());
		SimpleAuthorizationInfo info =  new SimpleAuthorizationInfo();
		info.addObjectPermissions(permissions);
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		return new SingleAuthenticationInfo(DevAuthConfig.USER.getId(),token.getCredentials());
	}
	
	/**
	 * Inner class that represents a permission, that is always valid.
	 */
	private static class AllGrantedPermission implements Permission {
		@Override
		public boolean implies(Permission permission) {
			return true;
		}
	}
	
	/**
	 * Inner class that matches any credentials.
	 */
	private static class AllGrantedCredentialsMatcher implements CredentialsMatcher{

		@Override
		public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
			return true;
		}
		
	}

}
