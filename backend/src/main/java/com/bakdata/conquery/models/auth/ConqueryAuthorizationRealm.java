package com.bakdata.conquery.models.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * This realms only provides authorization information for a given {@link UserId}.
 * For now there is only one such authorizing realm. This queries the {@link MetaStorage}.
 */
@RequiredArgsConstructor
public class ConqueryAuthorizationRealm extends AuthorizingRealm {
	
	public final MetaStorage storage;
	
	@Override
	protected void onInit() {
		super.onInit();
		/*
		 * We don't handle authentication here, thus no token is supported. However we
		 * need to provide a TokenClass (that is used nowhere else), to not cause a
		 * NullPointerException (see AuthenticatingRealm#supports).
		 */
		this.setAuthenticationTokenClass(UnusedAuthenticationToken.class);
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Objects.requireNonNull(principals, "No principal info was provided");
		UserId userId = (UserId) principals.getPrimaryPrincipal();
		SimpleAuthorizationInfo info = new ConqueryAuthorizationInfo();

		info.addObjectPermissions(Collections.unmodifiableSet(storage.getUser(userId).getEffectivePermissions()));
		
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		// This Realm only authorizes
		return null;
	}
	
	/**
	 * Dummy class for the TokenClass that is signals that this realm does not authenticate.
	 */
	@SuppressWarnings("serial")
	private static class UnusedAuthenticationToken implements AuthenticationToken {

		@Override
		public Object getPrincipal() {
			throw new UnsupportedOperationException(String.format("This realm (%s) only handles authorization. So this token's functions should never be called.", this.getClass().getName()));
		}

		@Override
		public Object getCredentials() {
			throw new UnsupportedOperationException(String.format("This realm (%s) only handles authorization. So this token's functions should never be called.", this.getClass().getName()));
		}
		
	}
	
	/**
	 * This AuthorizationInfo handles the collection of large amounts of {@link Permission}s by wrapping collections into a view
	 * instead of running an iterator over them. This also prevents a {@link ConcurrentModificationException} which occurred when 
	 * Permission were collected
	 */
	@SuppressWarnings("serial")
	public static class ConqueryAuthorizationInfo extends SimpleAuthorizationInfo {
		@Override
		public void addRole(String role) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addRoles(Collection<String> roles) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addStringPermission(String permission) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addStringPermissions(Collection<String> permissions) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addObjectPermission(Permission permission) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addObjectPermissions(Collection<Permission> permissions) {
			if (!(permissions instanceof Set)) {
				super.addObjectPermissions(permissions);
			}
			if (objectPermissions == null) {
				objectPermissions = (Set<Permission>) permissions;
				return;
			}
			objectPermissions = Sets.union(objectPermissions, (Set<Permission>)permissions);
		}
		
	}

	// We override this to work only on SetViews and allocate new sets
	@Override
	protected Collection<Permission> getPermissions(AuthorizationInfo info) {
		// The AuthorizationInfo must be a ConqueryAuthorizationInfo and should only hold ObjectPermissions
		Collection<Permission> perms = info.getObjectPermissions();

		if (perms.isEmpty()) {
			return Collections.emptySet();
		} else {
			return perms;
		}
	}

}
