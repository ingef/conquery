package com.bakdata.conquery.models.auth.subjects;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.HasCompactedAbilities;
import com.bakdata.conquery.models.auth.permissions.PermissionMixin;
import com.bakdata.conquery.models.auth.permissions.WildcardPermissionWrapper;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T>{

	/**
	 * This getter is only used for the JSON serialization/deserialization.
	 */
	@Getter(value = AccessLevel.PUBLIC, onMethod = @__({@Deprecated}))
	private final Set<ConqueryPermission> permissions = Collections.synchronizedSet(new HashSet<>());
	@Getter(value = AccessLevel.PUBLIC, onMethod = @__({@Deprecated}))
	private final Set<PermissionMixin> sPermissions = Collections.synchronizedSet(new HashSet<>());
	
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
	public Set<PermissionMixin> addPermissions(MasterMetaStorage storage, Set<PermissionMixin> permissions) throws JSONException {
		HashSet<PermissionMixin> addedPermissions = new HashSet<>();
		for(Permission permission : permissions) {
			if(permission instanceof ConqueryPermission) {				
				addedPermissions.add(addPermission(storage, (ConqueryPermission) permission));
			}
			if(permission instanceof PermissionMixin) {
				addedPermissions.add(addPermission(storage, (PermissionMixin) permission));				
			}
		}
		return addedPermissions;
	}
	
	public synchronized PermissionMixin addPermission(MasterMetaStorage storage, PermissionMixin permission) throws JSONException {
		sPermissions.add(permission);
		updateStorage(storage);
		return permission;
	}

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
	public synchronized ConqueryPermission addPermission(MasterMetaStorage storage, ConqueryPermission permission) throws JSONException {

		Optional<ConqueryPermission> similar = permission.findSimilar(permissions);

		if (similar.isPresent() && similar.get() instanceof HasCompactedAbilities) {
			// found same/similar permission
			ConqueryPermission existingPermission = similar.get();
			if (existingPermission.equals(permission)) {
				// is actually the same permission
				log.info("User {} has already permission {}.", this, permission);
				return permission;
			}
			// new permission has different ability
			
			// remove old permission because we construct and add a new one
			permissions.remove(existingPermission);
			((HasCompactedAbilities)existingPermission).addAbilities(((HasCompactedAbilities) permission).getAbilitiesCopy());
			log.info("Compected permission: {}",existingPermission);

			permission = existingPermission;
			
		}

		permissions.add(permission);
		updateStorage(storage);
		return permission;
	}
	
	public void removePermission(MasterMetaStorage storage, Permission delPermission) throws JSONException {
		synchronized (sPermissions) {
			sPermissions.remove(delPermission);
		}
		this.updateStorage(storage);
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
			Optional<ConqueryPermission> similar =  delPermission.findSimilar(permissions);
			if (similar.isPresent()) {
				// found permission with the same target
				ConqueryPermission permission = similar.get();
				/*
				 *  We must remove the complete permission from the HashSet,
				 *  because we will probably change the object and the corresponding has
				 */
				permissions.remove(permission);
				
				// Handle permissions with multiple abilities 
				if(permission instanceof HasCompactedAbilities) {
					if(!(delPermission instanceof HasCompactedAbilities)) {
						throw new IllegalStateException(String.format("Permissions are not of same type. Got existing %s and removing %s.", permission, delPermission));
					}
					HasCompactedAbilities iPerm = (HasCompactedAbilities) permission;
					HasCompactedAbilities iDelPerm = (HasCompactedAbilities) delPermission;
					// remove all provided abilities
					if(iPerm.removeAllAbilities(iDelPerm.getAbilitiesCopy())) {
						log.info(String.format("After deleting the abilites %s the permission remains as: %s", iDelPerm.getAbilitiesCopy(), permission));
					}
					
					
					// if there are abilities left, add the permission back to the local storage
					if(!iPerm.getAbilitiesCopy().isEmpty()) {
						permissions.add(permission);
					}
				}
				// make the change persistent
				this.updateStorage(storage);
			}
		}
	}

	/**
	 * Return a copy of the permissions hold by the owner.
	 * @return A set of the permissions hold by the owner.
	 */
	@JsonIgnore
	public Set<PermissionMixin> getPermissionsCopy(){
		HashSet<PermissionMixin> permissionsCopy = new HashSet<PermissionMixin>(this.permissions);
		permissionsCopy.addAll(sPermissions);
		return permissionsCopy;
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
	public abstract Set<PermissionMixin> getPermissionsEffective();
	
	/**
	 * Update this instance, only to be called from a synchronized context.
	 * @throws JSONException 
	 */
	protected abstract void updateStorage(MasterMetaStorage storage) throws JSONException;

}
