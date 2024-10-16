package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

/**
 * A group consists of users and permissions. The permissions held by the group
 * are effective for all users in the group. In Conquery, when a user shares a
 * query it is currently shared with all groups a user is in.
 */
@Slf4j
public class Group extends PermissionOwner<GroupId> implements RoleOwner {

	@JsonProperty
	private Set<UserId> members = Collections.synchronizedSet(new HashSet<>());
	@JsonProperty
	private Set<RoleId> roles = Collections.synchronizedSet(new HashSet<>());

	@JsonCreator
	private Group(String name, String label) {
		this(name, label, null);
	}

	public Group(String name, String label, MetaStorage storage) {
		super(name, label, storage);
	}

	@Override
	public Set<ConqueryPermission> getEffectivePermissions() {
		Set<ConqueryPermission> permissions = getPermissions();
		for (RoleId roleId : roles) {
			permissions = Sets.union(permissions, getMetaStorage().getRole(roleId).getEffectivePermissions());
		}
		return permissions;
	}


	public synchronized void addMember(UserId user) {
		if (members.add(user)) {
			log.trace("Added user {} to group {}", user, getId());
			updateStorage();
		}
	}

	@Override
	public void updateStorage() {
		getMetaStorage().updateGroup(this);
	}

	@Override
	public GroupId createId() {
		return new GroupId(name);
	}

	public synchronized void removeMember(UserId user) {
		if (members.remove(user)) {
			log.trace("Removed user {} from group {}", user, getId());
			updateStorage();
		}
	}

	public boolean containsUser(UserId user) {
		return members.contains(user);
	}

	public Set<UserId> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public synchronized void addRole(RoleId role) {
		if (roles.add(role)) {
			log.trace("Added role {} to group {}", role, getId());
			updateStorage();
		}
	}

	public synchronized void removeRole(RoleId role) {
		if (roles.remove(role)) {
			log.trace("Removed role {} from group {}", role, getId());
			updateStorage();
		}
	}

	public Set<RoleId> getRoles() {
		return Collections.unmodifiableSet(roles);
	}
}
