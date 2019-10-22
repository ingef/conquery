package com.bakdata.conquery.models.auth.subjects;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The base class of security subjects in this project. Used to represent
 * persons and groups with permissions.
 *
 * @param <T>
 *            The id type by which an instance is identified
 */
@Slf4j
@JsonIgnoreProperties({ "session", "previousPrincipals", "runAs", "principal", "authenticated", "remembered", "principals" })
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T> implements Subject {

	/**
	 * This getter is only used for the JSON serialization/deserialization
	 */
	@Getter(value = AccessLevel.PUBLIC, onMethod = @__({@Deprecated}))
	private final Set<ConqueryPermission> permissions = new HashSet<>();

	@Override
	public Object getPrincipal() {
		return getId();
	}

	@Override
	public PrincipalCollection getPrincipals() {
		return new SinglePrincipalCollection(getId());
	}

	@Override
	public boolean isPermitted(String permission) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] isPermitted(String... permissions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPermittedAll(String... permissions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkPermission(String permission) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkPermissions(String... permissions) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkPermission(Permission permission) throws AuthorizationException {
		if (!(permission instanceof ConqueryPermission)) {
			throw new AuthorizationException("Supplied permission " + permission + " is not of Type " + ConqueryPermission.class.getName());
		}
		SecurityUtils.getSecurityManager().checkPermission(getPrincipals(), permission);
	}

	@Override
	public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
		for (Permission permission : permissions) {
			if (!(permission instanceof ConqueryPermission)) {
				throw new AuthorizationException(
					"Supplied permission " + permission + " is not of Type " + ConqueryPermission.class.getName());
			}
			SecurityUtils.getSecurityManager().checkPermission(getPrincipals(), permission);
		}
	}

	@Override
	public boolean hasRole(String roleIdentifier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] hasRoles(List<String> roleIdentifiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAllRoles(Collection<String> roleIdentifiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRole(String roleIdentifier) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRoles(Collection<String> roleIdentifiers) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRoles(String... roleIdentifiers) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(AuthenticationToken token) throws AuthenticationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Session getSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Session getSession(boolean create) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> V execute(Callable<V> callable) throws ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute(Runnable runnable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> Callable<V> associateWith(Callable<V> callable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Runnable associateWith(Runnable runnable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void runAs(PrincipalCollection principals) throws NullPointerException, IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRunAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrincipalCollection getPreviousPrincipals() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrincipalCollection releaseRunAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAuthenticated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRemembered() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Adds a permission to the storage and to the locally stored permissions by
	 * calling indirectly {@link #addPermissionLocal(ConqueryPermission)}.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission (Id might change when the owner changed
	 *         or permissions are aggregated)
	 * @throws JSONException
	 *             When the permission object could not be formed in to the
	 *             appropriate JSON format.
	 */
	public synchronized ConqueryPermission addPermission(MasterMetaStorage storage, ConqueryPermission permission) throws JSONException {

		Optional<ConqueryPermission> sameTarget = ofTarget(permission);

		if (sameTarget.isPresent()) {
			// found permission with the same target
			ConqueryPermission oldPermission = sameTarget.get();
			if (oldPermission.equals(permission)) {
				// is actually the same permission
				log.info("User {} has already permission {}.", this, permission);
				return permission;
			}
			else {
				// new permission has different ability
				List<ConqueryPermission> reducedPermission = ConqueryPermission
					.reduceByOwnerAndTarget(Arrays.asList(oldPermission, permission));
				removePermission(storage, oldPermission);
				// has only one entry as permissions only differ in the ability
				permission = reducedPermission.get(0);
			}
		}

		permissions.add(permission);
		updateStorage(storage);
		return permission;
	}

	/**
	 * Removes a permission from the storage and from the locally stored permissions
	 * by calling.
	 *
	 * @param storage
	 *            The storage in which the permission persists.
	 * @param permission
	 *            The permission to be deleted.
	 * @throws JSONException 
	 */
	public void removePermission(MasterMetaStorage storage, ConqueryPermission delPermission) throws JSONException {
		synchronized (permissions) {
			Optional<ConqueryPermission> sameTarget =  ofTarget(delPermission);
			if (sameTarget.isPresent()) {
				// found permission with the same target
				ConqueryPermission permission = sameTarget.get();
				
				// remove all provided abilities
				EnumSet<Ability> abilities = permission.getAbilities();
				abilities.removeAll(permission.getAbilities());
				
				// if no abilitiy is left, remove the whole permission
				if(abilities.isEmpty()) {
					permissions.remove(permission);
				}
				this.updateStorage(storage);
			}
		}
	}

	private Optional<ConqueryPermission> ofTarget(ConqueryPermission other) {
		Iterator<ConqueryPermission> it = permissions.iterator();
		while (it.hasNext()) {
			ConqueryPermission perm = it.next();
			if (perm.getTarget().equals(other.getTarget())) {
				return Optional.of(perm);
			}
		}
		return Optional.empty();

	}

	/**
	 * Owns the permission and checks if it is permitted by only regarding owner
	 * specific permissions. Inherit permission from roles are not checked.
	 *
	 * @param permission
	 *            The permission to check.
	 */
	public boolean isPermittedSelfOnly(ConqueryPermission permission) {
		return SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permission);
	}
	/**
	 * Return a copy of the permissions hold by the owner.
	 * @return A set of the permissions hold by the owner.
	 */
	@JsonIgnore
	public Set<ConqueryPermission> getPermissionsCopy(){
		return new HashSet<ConqueryPermission>(permissions);
	}
	
	public void setPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissionsNew) throws JSONException {
		synchronized (permissions) {
			permissions.clear();
			permissions.addAll(permissionsNew);
			updateStorage(storage);
		}
	}
	
	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits.
	 * @return
	 */
	public abstract Set<ConqueryPermission> getPermissionsEffective();
	
	/**
	 * Update this instance, only to be called from a synchronized context.
	 * @throws JSONException 
	 */
	protected abstract void updateStorage(MasterMetaStorage storage) throws JSONException;

}
