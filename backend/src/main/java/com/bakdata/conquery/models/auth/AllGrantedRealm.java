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

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AllGrantedRealm extends AuthorizingRealm {
	private static final String PRINCIPAL = "SUPERUSER@ALLGRANTEDREALM.DE";
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
	
	private static final UserId ID= new UserId(PRINCIPAL);
	private static final String LABEL = "SUPERUSER";
	
	public AllGrantedRealm(MasterMetaStorage storage) {
		log.warn(WARNING);
		this.setAuthenticationTokenClass(ConqueryToken.class);
		this.setCredentialsMatcher(new AllGrantedCredentialsMatcher());
		User user = new User(new SinglePrincipalCollection(ID));
		user.setStorage(storage);
		user.setName(LABEL);
		user.setLabel(LABEL);
		try {
			storage.updateUser(user);
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
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
		return new SingleAuthenticationInfo(ID,token.getCredentials());
	}
	
	private static class AllGrantedPermission implements Permission {
		@Override
		public boolean implies(Permission permission) {
			return true;
		}
	}
	
	private static class AllGrantedCredentialsMatcher implements CredentialsMatcher{

		@Override
		public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
			return true;
		}
		
	}

}
