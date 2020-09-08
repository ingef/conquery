package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The base class of security subjects in this project. Used to represent
 * persons and groups with permissions.
 *
 * @param <T>
 *            The id type by which an instance is identified
 */
@Slf4j
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T> implements Comparable<PermissionOwner<?>> {
	
	private static final Comparator<PermissionOwner<?>> COMPARATOR = Comparator.<PermissionOwner<?>, String>comparing(PermissionOwner::getLabel).thenComparing(po -> po.getId().toString());

	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	protected String name;
	
	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	protected String label;
	
	private final Set<ConqueryPermission> permissions = Collections.synchronizedSet(new HashSet<>());
	
	
	public PermissionOwner(String name, String label) {
		this.name = name;
		this.label = label;
	}
	

	/**
	 * Adds permissions to the owner object and to the persistent storage.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission
	 */
	public Set<ConqueryPermission> addPermissions(MetaStorage storage, Set<ConqueryPermission> permissions) {
		HashSet<ConqueryPermission> addedPermissions = new HashSet<>();
		for (ConqueryPermission permission : permissions) {
			addedPermissions.add(addPermission(storage, permission));
		}
		return addedPermissions;
	}

	public ConqueryPermission addPermission(MetaStorage storage, ConqueryPermission permission) {
		if (permissions.add(permission)) {
			updateStorage(storage);
			log.trace("Added permission {} to owner {}", permission, getId());
		}
		return permission;
	}
	
	/**
	 * Removes permissions from the owner object and from the persistent storage.
	 *
	 * @param storage
	 *            A storage where the permission are added for persistence.
	 * @param permission
	 *            The permission to add.
	 * @return Returns the added Permission
	 */
	public void removePermissions(MetaStorage storage, Set<ConqueryPermission> permissions) {
		for (ConqueryPermission permission : permissions) {
			removePermission(storage, permission);
		}
	}

	public void removePermission(MetaStorage storage, Permission delPermission) {
		if (permissions.remove(delPermission)) {
			this.updateStorage(storage);
			log.trace("Removed permission {} from owner {}", delPermission, getId());
		}
	}

	/**
	 * Returns a copy of the permissions hold by the owner.
	 * Changes to the returned collection are not persisted and do not alter the
	 * permissions owned.
	 * 
	 * @return A set of the permissions hold by the owner.
	 */
	public Set<Permission> getPermissions() {
		// HashSet uses internally an iterator for copying, so we need to synchronize this
		synchronized (permissions) {
			if (permissions.isEmpty()) {
				return Collections.emptySet();
			}
			return new HashSet<>(permissions);
		}
	}
	
	/**
	 * Custom property setter for permissions so that the existing Hashset is not replaced by Jackson and
	 * the references held by the members {@link PemissionOwner#unmodifiablePermissions} and {@link PermissionOwner#synchronizedUnmodifiablePermissions}
	 * are still valid.
	 * @param permissions
	 */
	@JsonProperty
	private void setPermissions(Set<ConqueryPermission> permissions) {
		this.permissions.clear();
		this.permissions.addAll(permissions);
	}

	public void setPermissions(MetaStorage storage, Set<ConqueryPermission> permissionsNew) {
		permissions.clear();
		permissions.addAll(permissionsNew);
		updateStorage(storage);
	}

	/**
	 * Update this instance in the {@link MetaStorage}.
	 */
	protected abstract void updateStorage(MetaStorage storage);
	
	
	@Override
	public int compareTo(PermissionOwner<?> other) {
		return COMPARATOR.compare(this, other);
	}

}
