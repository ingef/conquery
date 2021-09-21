package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A group consists of users and permissions. The permissions held by the group
 * are effective for all users in the group. In Conquery, when a user shares a
 * query it is currently shared with all groups a user is in.
 *
 */
@Slf4j
public class Group extends PermissionOwner<GroupId> implements RoleOwner {

	@JsonProperty
	private Set<UserId> members = Collections.synchronizedSet(new HashSet<>());
	@JsonProperty
	private Set<RoleId> roles = Collections.synchronizedSet(new HashSet<>());


	@JacksonInject(useInput = OptBoolean.FALSE)
	@NonNull
	@EqualsAndHashCode.Exclude
	protected StorageUpdater storageUpdater;

	public Group(String name, String label, Consumer<Group> storageUpdater) {
		super(name, label);
		this.storageUpdater = new StorageUpdater(storageUpdater);
	}

	@Override
	protected void updateStorage() {
		storageUpdater.accept(this);
	}

	@Override
	public GroupId createId() {
		return new GroupId(name);
	}

	public synchronized void addMember(User user) {
		if(members.add(user.getId())) {
			log.trace("Added user {} to group {}", user.getId(), getId());
			updateStorage();
		}
	}

	public synchronized void removeMember(User user) {
		if (members.remove(user.getId())) {
			log.trace("Removed user {} from group {}", user.getId(), getId());
			updateStorage();
		}
	}

	public boolean containsMember(User user) {
		return members.contains(user.getId());
	}

	public Set<UserId> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public synchronized void addRole(Role role) {
		if (roles.add(role.getId())) {
			log.trace("Added role {} to group {}", role.getId(), getId());
			updateStorage();
		}
	}

	public synchronized void removeRole(Role role) {
		if (roles.remove(role.getId())) {
			log.trace("Removed role {} from group {}", role.getId(), getId());
			updateStorage();
		}
	}

	public Set<RoleId> getRoles() {
		return Collections.unmodifiableSet(roles);
	}



	@RequiredArgsConstructor
	public static class StorageUpdater implements MetaStorage.StorageUpdater<Group> {

		private final Consumer<Group> storageUpdater;

		@Override
		public void accept(Group group) {
			storageUpdater.accept(group);
		}

		@Override
		public MutableInjectableValues inject(MutableInjectableValues values) {
			values.add(Group.StorageUpdater.class, this);
			return values;
		}
	}
}
