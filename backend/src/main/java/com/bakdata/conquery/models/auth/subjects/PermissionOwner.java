package com.bakdata.conquery.models.auth.subjects;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
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

		Optional<ConqueryPermission> sameTarget = ofSameTypeAndTarget(permission);

		if (sameTarget.isPresent()) {
			// found permission with the same target
			ConqueryPermission oldPermission = sameTarget.get();
			if (oldPermission.equals(permission)) {
				// is actually the same permission
				log.info("User {} has already permission {}.", this, permission);
				return permission;
			}
			// new permission has different ability
			
			// remove old permission because we construct and add a new one
			permissions.remove(oldPermission);
			List<ConqueryPermission> reducedPermission = ConqueryPermission
				.reduceByTarget(Arrays.asList(oldPermission, permission));
			// has only one entry as permissions only differ in the ability
			permission = reducedPermission.get(0);
			
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
			Optional<ConqueryPermission> sameTarget =  ofSameTypeAndTarget(delPermission);
			if (sameTarget.isPresent()) {
				// found permission with the same target
				ConqueryPermission permission = sameTarget.get();
				/*
				 *  We must remove the complete permission from the HashSet,
				 *  because we will probably change the object and the corresponding has
				 */
				permissions.remove(permission);
				
				// remove all provided abilities
				if(permission.removeAllAbilities(delPermission.getAbilitiesCopy())) {
					log.info(String.format("After deleting the abilites %s the permission remains as: %s", delPermission.getAbilitiesCopy(), permission));
				}
				
				
				// if there are abilities left, add the permission back to the local storage
				if(!permission.getAbilitiesCopy().isEmpty()) {
					permissions.add(permission);
				}
				// make the change persistent
				this.updateStorage(storage);
			}
		}
	}

	private Optional<ConqueryPermission> ofSameTypeAndTarget(ConqueryPermission other) {
		Iterator<ConqueryPermission> it = permissions.iterator();
		while (it.hasNext()) {
			ConqueryPermission perm = it.next();
			if(!perm.getClass().isAssignableFrom(other.getClass())) {
				continue;
			}
			if (!perm.getTarget().equals(other.getTarget())) {
				continue;
			}
			return Optional.of(perm);
		}
		return Optional.empty();

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
