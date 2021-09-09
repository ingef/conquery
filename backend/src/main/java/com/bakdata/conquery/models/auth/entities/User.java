package com.bakdata.conquery.models.auth.entities;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class User extends PermissionOwner<UserId> implements Principal, RoleOwner, Userish {

	@JsonProperty
	private final Set<RoleId> roles = Collections.synchronizedSet(new HashSet<>());

	@Getter
	@Setter
	@JsonIgnore
	private transient boolean displayLogout = true;

	// protected for testing purposes
	@JsonIgnore
	@Getter(AccessLevel.PROTECTED)
	private final transient ShiroUserAdapter shiroUserAdapter;

	public User(String name, String label, MetaStorage storage) {
		super(name, label, storage);
		this.shiroUserAdapter = new ShiroUserAdapter();
	}

	@Override
	public UserId createId() {
		return new UserId(name);
	}

	public synchronized void addRole(Role role) {
		if (roles.add(role.getId())) {
			log.trace("Added role {} to user {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	@Override
	public synchronized void removeRole(Role role) {
		if (roles.remove(role.getId())) {
			log.trace("Removed role {} from user {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	public Set<RoleId> getRoles() {
		return Collections.unmodifiableSet(roles);
	}

	@Override
	protected void updateStorage(MetaStorage storage) {
		storage.updateUser(this);
	}

	public void authorize(@NonNull Authorized object, @NonNull Ability ability) {
		if (isOwner(object)) {
			return;
		}

		shiroUserAdapter.checkPermission(object.createPermission(EnumSet.of(ability)));
	}

	public void authorize(Set<? extends Authorized> objects, Ability ability) {
		for (Authorized object : objects) {
			authorize(object, ability);
		}
	}

	public boolean isPermitted(Authorized object, Ability ability) {
		if (isOwner(object)) {
			return true;
		}

		return shiroUserAdapter.isPermitted(object.createPermission(EnumSet.of(ability)));
	}


	public boolean isPermittedAll(Collection<? extends Authorized> authorized, Ability ability) {
		return authorized.stream()
						 .allMatch(auth -> isPermitted(auth, ability));
	}


	public boolean[] isPermitted(List<? extends Authorized> authorizeds, Ability ability) {
		return authorizeds.stream()
						  .map(auth -> isPermitted(auth, ability))
						  .collect(Collectors.toCollection(BooleanArrayList::new))
						  .toBooleanArray();
	}


	public boolean isOwner(Authorized object) {
		return object instanceof Owned && equals(((Owned) object).getOwner());
	}

	/**
	 * This class is non static so its a fixed part of the enclosing User object.
	 * Its protected for testing purposes only.
	 */
	protected class ShiroUserAdapter extends FilteredUser {

		@Override
		public void checkPermission(Permission permission) throws AuthorizationException {
			SecurityUtils.getSecurityManager().checkPermission(getPrincipals(), permission);
		}

		@Override
		public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
			SecurityUtils.getSecurityManager().checkPermissions(getPrincipals(), permissions);
		}

		@Override
		public PrincipalCollection getPrincipals() {
			return new SinglePrincipalCollection(getId());
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


		@Override
		public Object getPrincipal() {
			return getId();
		}


	}
}
