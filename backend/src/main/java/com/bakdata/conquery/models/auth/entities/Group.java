package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
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

	public Group(String name, String label) {
		super(name, label);
	}

	@Override
	protected void updateStorage(MetaStorage storage) {
		storage.updateGroup(this);
	}

	@Override
	public GroupId createId() {
		return new GroupId(name);
	}

	public void addMember(MetaStorage storage, User user) {
		if(members.add(user.getId())) {
			log.trace("Added user {} to group {}", user.getId(), getId());
			updateStorage(storage);
		}
	}

	public void removeMember(MetaStorage storage, User user) {
		if(members.remove(user.getId())) {
			log.trace("Removed user {} from group {}", user.getId(), getId());				
			updateStorage(storage);
		}
	}

	public boolean containsMember(User user) {
		return members.contains(user.getId());
	}

	public Set<UserId> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public void addRole(MetaStorage storage, Role role) {
		if (roles.add(role.getId())) {
			log.trace("Added role {} to group {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	public void removeRole(MetaStorage storage, Role role) {
		if (roles.remove(role.getId())) {
			log.trace("Removed role {} from group {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	public Set<RoleId> getRoles() {
		return Collections.unmodifiableSet(roles);
	}
}
