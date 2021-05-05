package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

/**
 * The base class of security subjects in this project. Used to represent
 * persons and groups with permissions.
 *
 * @param <T> The id type by which an instance is identified
 */
@Slf4j
@EqualsAndHashCode(callSuper = false)
public abstract class PermissionOwner<T extends PermissionOwnerId<? extends PermissionOwner<T>>> extends IdentifiableImpl<T> implements Comparable<PermissionOwner<?>> {

	private static final Comparator<PermissionOwner<?>>
			COMPARATOR =
			Comparator.<PermissionOwner<?>, String>comparing(PermissionOwner::getLabel).thenComparing(po -> po.getId().toString());

	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	@ToString.Include
	protected String name;

	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	@ToString.Include
	protected String label;

	@ToString.Exclude
	@Getter(AccessLevel.PRIVATE) // So only Jackson can use this to deserialize
	@NotNull
	private Set<ConqueryPermission> permissions = new HashSet<>();


	public PermissionOwner(String name, String label) {
		this.name = name;
		this.label = label;
	}


	/**
	 * Adds permissions to the owner object and to the persistent storage.
	 *
	 * @param storage     A storage where the permission are added for persistence.
	 * @param permissions The permissions to add.
	 * @return Returns the added Permission
	 */
	public boolean addPermissions(MetaStorage storage, Set<ConqueryPermission> permissions) {
		boolean ret = false;
		synchronized (this) {
			this.permissions = ImmutableSet
									   .<ConqueryPermission>builder()
									   .addAll(this.permissions)
									   .addAll(permissions)
									   .build();
			updateStorage(storage);
		}
		return ret;
	}

	public boolean addPermission(MetaStorage storage, ConqueryPermission permission) {
		boolean ret = false;
		synchronized (this) {
			this.permissions = ImmutableSet
									   .<ConqueryPermission>builder()
									   .addAll(this.permissions)
									   .add(permission)
									   .build();
			updateStorage(storage);
		}
		return ret;
	}

	/**
	 * Removes permissions from the owner object and from the persistent storage.
	 *
	 * @param storage     A storage where the permission are saved for persistence.
	 * @param permissions The permission to remove.
	 * @return Returns the added Permission
	 */
	public boolean removePermissions(MetaStorage storage, Set<ConqueryPermission> permissions) {
		boolean ret = false;
		synchronized (this) {
			Set<ConqueryPermission> newSet = new HashSet<>(this.permissions);
			ret = newSet.removeAll(permissions);
			this.permissions = newSet;
			updateStorage(storage);
		}
		return ret;
	}

	public boolean removePermission(MetaStorage storage, Permission permission) {
		boolean ret = false;
		synchronized (this) {
			Set<ConqueryPermission> newSet = new HashSet<>(this.permissions);
			ret = newSet.remove(permission);
			this.permissions = newSet;
			updateStorage(storage);
		}
		return ret;
	}

	/**
	 * Returns a copy of the permissions hold by the owner.
	 * Changes to the returned collection are not persisted and do not alter the
	 * permissions owned.
	 *
	 * @return A set of the permissions hold by the owner.
	 */
	public Set<ConqueryPermission> getPermissions() {
		if (permissions.isEmpty()) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(permissions);

	}

	public void setPermissions(MetaStorage storage, Set<ConqueryPermission> permissionsNew) {
		synchronized (this) {
			Set<ConqueryPermission> newSet = new HashSet<>(permissionsNew.size());
			newSet.addAll(permissionsNew);
			this.permissions = newSet;
			updateStorage(storage);
		}
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
