package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
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
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T>{

	private final Set<ConqueryPermission> permissions = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * Adds permissions to the user and persistent to the storage.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission (Might change when the permissions are aggregated)
	 * @throws JSONException
	 *             When the permission object could not be formed in to the
	 *             appropriate JSON format.
	 */
	public Set<ConqueryPermission> addPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissions) throws JSONException {
		HashSet<ConqueryPermission> addedPermissions = new HashSet<>();
		for(ConqueryPermission permission : permissions) {
			addedPermissions.add(addPermission(storage, permission));
		}
		return addedPermissions;
	}
	
	public ConqueryPermission addPermission(MasterMetaStorage storage, ConqueryPermission permission) throws JSONException {
		if(permissions.add(permission)) {			
			updateStorage(storage);
			log.trace("Added permission {} to owner {}", permission, getId());	
		}
		return permission;
	}

	public void removePermission(MasterMetaStorage storage, Permission delPermission) throws JSONException {
		if(permissions.remove(delPermission)) {
			this.updateStorage(storage);
			log.trace("Removed permission {} from owner {}", delPermission, getId());		
		}
	}


	/**
	 * Return as immutable copy of the permissions hold by the owner.
	 * @return A set of the permissions hold by the owner.
	 */
	public Set<ConqueryPermission> getPermissions(){
		synchronized (permissions) {			
			return Set.copyOf(permissions);
		}
	}
	
	public void setPermissions(MasterMetaStorage storage, Set<ConqueryPermission> permissionsNew) throws JSONException {
		permissions.clear();
		permissions.addAll(permissionsNew);
		updateStorage(storage);
	}
	
	/**
	 * Update this instance, only to be called from a synchronized context.
	 * @throws JSONException upon serialization error.
	 */
	protected abstract void updateStorage(MasterMetaStorage storage) throws JSONException;

}
