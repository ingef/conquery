package com.bakdata.conquery.models.auth.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.models.auth.AuthorizationStorage;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * A group consists of users and permissions. The permissions held by the group
 * are effective for all users in the group. In Conquery, when a user shares a
 * query it is currently shared with all groups a user is in.
 *
 */
@Slf4j
public class Group extends PermissionOwner<GroupId> implements RoleOwner {

	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	private String name;
	@Getter
	@Setter
	@NonNull
	@NotNull
	@NotEmpty
	private String label;

	@MetaIdRefCollection
	private Set<User> members = Collections.synchronizedSet(new HashSet<>());
	@MetaIdRefCollection
	private Set<Role> roles = Collections.synchronizedSet(new HashSet<>());

	public Group(String name, String label) {
		this.name = name;
		this.label = label;
	}

	@Override
	protected void updateStorage(AuthorizationStorage storage) {
		storage.updateGroup(this);
	}

	@Override
	public GroupId createId() {
		return new GroupId(name);
	}

	public void addMember(AuthorizationStorage storage, User user) throws JSONException {
		if(members.add(user)) {
			log.trace("Added user {} to group {}", user.getId(), getId());
			updateStorage(storage);
		}
	}

	public void removeMember(AuthorizationStorage storage, User user) throws JSONException {
		if(members.remove(user)) {
			log.trace("Removed user {} from group {}", user.getId(), getId());				
			updateStorage(storage);
		}
	}

	public boolean containsMember(User user) {
		return members.contains(user);
	}

	public Set<User> getMembers() {
		return Set.copyOf(members);
	}

	public void addRole(AuthorizationStorage storage, Role role) throws JSONException {
		if (roles.add(role)) {
			log.trace("Added role {} to group {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	public void removeRole(AuthorizationStorage storage, Role role) {
		if (roles.remove(role)) {
			log.trace("Removed role {} from group {}", role.getId(), getId());
			updateStorage(storage);
		}
	}

	public Set<Role> getRoles() {
		return Set.copyOf(roles);
	}

}
