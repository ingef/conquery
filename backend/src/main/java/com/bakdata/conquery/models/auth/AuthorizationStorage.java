package com.bakdata.conquery.models.auth;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public interface AuthorizationStorage {
	
	// ROLES
	Role getRole(RoleId roleId);
	List<Role> getAllRoles();	
	void addRole(Role role);
	void addRoles(List<Role> roles);
	/**
	 * Deletes the role, that is identified by the id. Its references are
	 * removed from the users and from the storage.
	 *
	 * @param mandatorId
	 *            The id belonging to the mandator
	 * @throws JSONException
	 *             is thrown on JSON validation form the storage.
	 */
	void removeRole(RoleId roleId);


	User getUser(UserId userId);
	List<User> getUsersByRole(Role role);
	List<User> getAllUsers();
	void addUser(User user);
	void addUsers(List<User> users);
	void deleteUser(UserId userId);

	Collection<Group> getAllGroups();
	void addGroup(Group group);
	void addGroups(List<Group> groups);
	void removeGroup(GroupId groupId);

	void updateUser(User user);
	void updateRole(Role role);
	void updateGroup(Group group);
	Group getGroup(GroupId groupId);

}
