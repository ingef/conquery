package com.bakdata.conquery.models.auth;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.auth.util.SingleAuthenticationInfo;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

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
	
	private final MasterMetaStorage storage;
	
	/**
	 * Standard constructor.
	 */
	public AllGrantedRealm(MasterMetaStorage storage) {
		log.warn(WARNING);
		this.setAuthenticationTokenClass(AuthenticationToken.class);
		this.storage = storage;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Objects.requireNonNull(principals, "No principal info was provided");
		UserId userId = UserId.class.cast(principals.getPrimaryPrincipal());
		SimpleAuthorizationInfo info =  new SimpleAuthorizationInfo();
		
		if(userId.equals(DevAuthConfig.USER.getId())) {
			// It's the default superuser, give her/him the ultimate permission
			info.addObjectPermissions(Set.of(new SuperPermission()));
		} else {
			// currently only used for test cases
			info.addObjectPermissions(new HashSet<Permission>(userId.getOwner(storage).getEffectivePermissions()));
		}
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		// Authenticate every token as the superuser
		return new SingleAuthenticationInfo(DevAuthConfig.USER.getId(),token.getCredentials());
	}
}
