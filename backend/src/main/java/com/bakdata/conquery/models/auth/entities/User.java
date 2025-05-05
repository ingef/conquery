package com.bakdata.conquery.models.auth.entities;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class User extends PermissionOwner<UserId> implements Principal, RoleOwner, Subject {

	@JsonProperty
	private final Set<RoleId> roles = Collections.synchronizedSet(new HashSet<>());

	// protected for testing purposes
	@JsonIgnore
	@Getter(AccessLevel.PROTECTED)
	private final transient ShiroUserAdapter shiroUserAdapter;

	@JsonCreator
	protected User(String name, String label) {
		this(name, label, null);
	}

	public User(String name, String label, MetaStorage storage) {
		super(name, label, storage);
		this.shiroUserAdapter = new ShiroUserAdapter();
	}

	@Override
	public Set<ConqueryPermission> getEffectivePermissions() {
		Set<ConqueryPermission> permissions = getPermissions();
		for (RoleId roleId : roles) {
			Role role = getMetaStorage().getRole(roleId);
			if (role == null) {
				log.warn("Could not find role {} to gather permissions", roleId);
				continue;
			}
			permissions = Sets.union(permissions, role.getEffectivePermissions());
		}

		try(Stream<Group> allGroups = getMetaStorage().getAllGroups()){

			for (Iterator<Group> it = allGroups.iterator(); it.hasNext(); ) {
				Group group = it.next();
				if (!group.containsMember(this)) {
					continue;
				}
				permissions = Sets.union(permissions, group.getEffectivePermissions());
			}
		}


		return permissions;
	}

	public synchronized void addRole(Role role) {
		if (roles.add(role.getId())) {
			log.trace("Added role {} to user {}", role.getId(), getId());
			updateStorage();
		}
	}

	@Override
	public void updateStorage() {
		getMetaStorage().updateUser(this);
	}

	@Override
	public synchronized void removeRole(RoleId role) {
		if (roles.remove(role)) {
			log.trace("Removed role {} from user {}", role, getId());
			updateStorage();
		}
	}

	public Set<RoleId> getRoles() {
		return Collections.unmodifiableSet(roles);
	}

	public void authorize(Set<? extends Authorized> objects, Ability ability) {
		for (Authorized object : objects) {
			authorize(object, ability);
		}
	}

	public void authorize(@NonNull Authorized object, @NonNull Ability ability) {
		if (isOwner(object)) {
			return;
		}

		shiroUserAdapter.checkPermission(object.createPermission(EnumSet.of(ability)));
	}

	public boolean isOwner(Authorized object) {
		return object instanceof Owned && getId().equals(((Owned) object).getOwner());
	}

	@Override
	public UserId createId() {
		UserId userId = new UserId(name);
		userId.setMetaStorage(getMetaStorage());
		return userId;
	}

	public boolean isPermittedAll(Collection<? extends Authorized> authorized, Ability ability) {
		return authorized.stream().allMatch(auth -> isPermitted(auth, ability));
	}

	public boolean isPermitted(Authorized object, Ability ability) {
		if (isOwner(object)) {
			return true;
		}

		return shiroUserAdapter.isPermitted(object.createPermission(EnumSet.of(ability)));
	}

	public boolean[] isPermitted(List<? extends Authorized> authorizeds, Ability ability) {
		boolean[] permitted = new boolean[authorizeds.size()];

		for (int index = 0; index < authorizeds.size(); index++) {
			permitted[index] = isPermitted(authorizeds.get(index), ability);
		}

		return permitted;
	}

	@JsonIgnore
	@Override
	public boolean isDisplayLogout() {
		return shiroUserAdapter.getAuthenticationInfo().get().isDisplayLogout();
	}

	@JsonIgnore
	@Override
	public ConqueryAuthenticationInfo getAuthenticationInfo() {
		return shiroUserAdapter.getAuthenticationInfo().get();
	}

	@JsonIgnore
	@Override
	public void setAuthenticationInfo(ConqueryAuthenticationInfo info) {
		shiroUserAdapter.getAuthenticationInfo().set(info);
	}

	@Override
	@JsonIgnore
	public User getUser() {
		return this;
	}

	/**
	 * This class is non-static, so it's a fixed part of the enclosing User object.
	 * It's protected for testing purposes only.
	 */
	@Getter
	public class ShiroUserAdapter extends FilteredUser {

		private final ThreadLocal<ConqueryAuthenticationInfo> authenticationInfo =
				ThreadLocal.withInitial(() -> new ConqueryAuthenticationInfo(User.this, null, null, false, null));

		@Override
		public Object getPrincipal() {
			return getId();
		}

		@Override
		public void checkPermission(Permission permission) throws AuthorizationException {
			SecurityUtils.getSecurityManager().checkPermission(getPrincipals(), permission);
		}

		@Override
		public PrincipalCollection getPrincipals() {
			return authenticationInfo.get().getPrincipals();
		}

		@Override
		public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
			SecurityUtils.getSecurityManager().checkPermissions(getPrincipals(), permissions);
		}

		@Override
		public boolean isPermitted(Permission permission) {
			return SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permission);
		}

		@Override
		public boolean[] isPermitted(List<Permission> permissions) {
			return SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permissions);
		}

		@Override
		public boolean isPermittedAll(Collection<Permission> permissions) {
			return SecurityUtils.getSecurityManager().isPermittedAll(getPrincipals(), permissions);
		}


	}


}
