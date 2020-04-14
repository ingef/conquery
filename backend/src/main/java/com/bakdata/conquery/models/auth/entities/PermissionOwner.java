package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

/**
 * The base class of security subjects in this project. Used to represent
 * persons and groups with permissions.
 *
 * @param <T>
 *            The id type by which an instance is identified
 */
@Slf4j
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T> {

	
	private final Set<ConqueryPermission> permissions = new HashSet<>();
	
	/** 
	 * Helper members to ensure that permissions are only modified using the add/remove methods and the storage is consistent.
	 */
	@JsonIgnore
	private final Set<Permission> unmodifiablePermissions = Collections.unmodifiableSet(permissions);
	@JsonIgnore
	private final Set<Permission> synchronizedUnmodifiablePermissions = Collections.synchronizedSet(unmodifiablePermissions);

	/**
	 * Adds permissions to the user and persistent to the storage.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission
	 */
	public Set<ConqueryPermission> addPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissions) {
		HashSet<ConqueryPermission> addedPermissions = new HashSet<>();
		for (ConqueryPermission permission : permissions) {
			addedPermissions.add(addPermission(storage, permission));
		}
		return addedPermissions;
	}

	public ConqueryPermission addPermission(MasterMetaStorage storage, ConqueryPermission permission) {
		synchronized (synchronizedUnmodifiablePermissions) {			
			if (permissions.add(permission)) {
				updateStorage(storage);
				log.trace("Added permission {} to owner {}", permission, getId());
			}
		}
		return permission;
	}

	public void removePermission(MasterMetaStorage storage, Permission delPermission) {
		synchronized (synchronizedUnmodifiablePermissions) {			
			if (permissions.remove(delPermission)) {
				this.updateStorage(storage);
				log.trace("Removed permission {} from owner {}", delPermission, getId());
			}
		}
	}

	/**
	 * Return as immutable copy of the permissions hold by the owner.
	 * 
	 * @return A set of the permissions hold by the owner.
	 */
	public Set<Permission> getPermissions() {
		// HashSet uses internally an iterator for copying, so we need to synchronize this
		return synchronizedUnmodifiablePermissions;
	}
	
	/**
	 * Custom property setter for permissions so that the existing Hashset is not replaced by Jackson and
	 * the references held by the members {@link PemissionOwner#unmodifiablePermissions} and {@link PermissionOwner#synchronizedUnmodifiablePermissions}
	 * are still valid.
	 * @param permissions
	 */
	@JsonProperty
	private void setPermissions(Set<ConqueryPermission> permissions) {
		synchronized (synchronizedUnmodifiablePermissions) {			
			this.permissions.clear();
			this.permissions.addAll(permissions);
		}
	}

	public void setPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissionsNew) {
		synchronized (synchronizedUnmodifiablePermissions) {			
			permissions.clear();
			permissions.addAll(permissionsNew);
			updateStorage(storage);
		}
	}

	/**
	 * Update this instance in the {@link MasterMetaStorage}.
	 */
	protected abstract void updateStorage(MasterMetaStorage storage);

}
