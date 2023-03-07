package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

/**
 * The base class of security subjects in this project. Used to represent
 * persons and groups with permissions.
 *
 * @param <T> The id type by which an instance is identified.
 * @implNote The NoArgsConstructor is private and used for deserialization
 */
@Slf4j
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
	@NotNull
	private final Set<ConqueryPermission> permissions = ConcurrentHashMap.newKeySet();

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@EqualsAndHashCode.Exclude
	protected MetaStorage storage;


	public PermissionOwner(String name, String label, MetaStorage storage) {
		this.name = name;
		this.label = label;
		this.storage = storage;
	}


	/**
	 * Adds permissions to the owner object and to the persistent storage.
	 *
	 * @param permissions The permissions to add.
	 * @return Returns the added Permission
	 */
	public synchronized void addPermissions(Set<ConqueryPermission> permissions) {
		this.permissions.addAll(permissions);
		updateStorage();
	}

	public synchronized void addPermission(ConqueryPermission permission) {
		permissions.add(permission);
		updateStorage();
	}

	/**
	 * Removes permissions from the owner object and from the persistent storage.
	 *
	 * @param permissions The permission to remove.
	 * @return Returns the added Permission
	 */
	public boolean removePermissions(Set<ConqueryPermission> permissions) {
		boolean ret = this.permissions.removeAll(permissions);
		updateStorage();

		return ret;
	}

	public boolean removePermission(Permission permission) {
		boolean ret = permissions.remove(permission);
		updateStorage();

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

	/**
	 * Returns a collection of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles/groups it inherits from.
	 *
	 * @return Owned and inherited permissions.
	 */
	@JsonIgnore
	public abstract Set<ConqueryPermission> getEffectivePermissions();

	public void updatePermissions(Set<ConqueryPermission> permissionsNew) {
		synchronized (this) {
			permissions.clear();
			permissions.addAll(permissionsNew);
			updateStorage();
		}
	}

	/**
	 * Update this instance in the {@link MetaStorage}.
	 */
	protected abstract void updateStorage();


	@Override
	public int compareTo(PermissionOwner<?> other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public abstract T createId();

	@Override
	public T getId() {
		return super.getId();
	}
}
