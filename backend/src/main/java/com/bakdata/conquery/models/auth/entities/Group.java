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
 *
 */
@Slf4j
public class Group extends PermissionOwner<GroupId> implements RoleOwner {

	@JsonProperty
	private Set<UserId> members = Collections.synchronizedSet(new HashSet<>());
	@JsonProperty
	private Set<RoleId> roles = Collections.synchronizedSet(new HashSet<>());

	@JsonCreator
	public Group(String name, String label) {
		this(name, label, null);
	}

	public Group(String name, String label, MetaStorage storage) {
		super(name, label, storage);
	}

	@Override
	public Set<ConqueryPermission> getEffectivePermissions() {
		Set<ConqueryPermission> permissions = getPermissions();
		for (RoleId roleId : roles) {
			permissions = Sets.union(permissions,storage.getRole(roleId).getEffectivePermissions());
		}
		return permissions;
	}

	@Override
	protected void updateStorage() {
		storage.updateGroup(this);
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
}
