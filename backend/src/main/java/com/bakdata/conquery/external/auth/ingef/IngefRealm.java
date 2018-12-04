package com.bakdata.conquery.external.auth.ingef;

import java.util.HashSet;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.ConqueryToken;
import com.bakdata.conquery.models.auth.SingleAuthenticationInfo;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IngefRealm extends AuthorizingRealm{
	
	private final IngefCredentialParser credentialParser = new IngefCredentialParser();
	private final MasterMetaStorage storage;
	
	public IngefRealm(MasterMetaStorage storage, String secret) {
		super();
		this.storage = storage;
		this.setAuthenticationTokenClass(ConqueryToken.class);
		this.credentialParser.setSecret(secret);
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(token instanceof ConqueryToken) {
			IngefCredentials info = credentialParser.parse(((ConqueryToken)token).getCredentials());
			isValid(info);

			return new SingleAuthenticationInfo(new UserId(info.getEmail()), token.getCredentials());
		}
		return null; // should never be reached, since shiro delegates only ConqueryTokens to this function
	}
	private static boolean isValid(IngefCredentials credentials) {
		boolean valid = credentials.isValid();
		if (!valid) {
			throw new AuthenticationException("Credentials expired. User: " + credentials.getEmail());
		}
		return valid;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		PermissionOwnerId<?> permissionOwner = (PermissionOwnerId<?>)principals.getPrimaryPrincipal();
		SimpleAuthorizationInfo info  = new SimpleAuthorizationInfo();
		info.addObjectPermissions(new HashSet<Permission>(storage.getPermissions(permissionOwner)));
		return info;
	}
}
