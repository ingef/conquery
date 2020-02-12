package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
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

	private final Set<ConqueryPermission> permissions = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Adds permissions to the user and persistent to the storage.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission (Might change when the permissions are aggregated)
	 */
	public Set<ConqueryPermission> addPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissions) {
		HashSet<ConqueryPermission> addedPermissions = new HashSet<>();
		for (ConqueryPermission permission : permissions) {
			addedPermissions.add(addPermission(storage, permission));
		}
		return addedPermissions;
	}

	public ConqueryPermission addPermission(MasterMetaStorage storage, ConqueryPermission permission) {
		if (permissions.add(permission)) {
			updateStorage(storage);
			log.trace("Added permission {} to owner {}", permission, getId());
		}
		return permission;
	}

	public void removePermission(MasterMetaStorage storage, Permission delPermission) {
		if (permissions.remove(delPermission)) {
			this.updateStorage(storage);
			log.trace("Removed permission {} from owner {}", delPermission, getId());
		}
	}

	/**
	 * Return as immutable copy of the permissions hold by the owner.
	 * 
	 * @return A set of the permissions hold by the owner.
	 */
	public Set<ConqueryPermission> getPermissions(){
		// HashSet uses internally an iterator for copying, so we need to synchronize this
		synchronized (permissions) {
			return Set.copyOf(permissions);
		}
	}

	public void setPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissionsNew) {
		permissions.clear();
		permissions.addAll(permissionsNew);
		updateStorage(storage);
	}

	/**
	 * Update this instance in the {@link MasterMetaStorage}.
	 */
	protected abstract void updateStorage(MasterMetaStorage storage);

}
