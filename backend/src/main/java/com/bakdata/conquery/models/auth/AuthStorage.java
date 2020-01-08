package com.bakdata.conquery.models.auth;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public interface AuthStorage {
	
	// ROLES
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
	void deleteRole(RoleId mandatorId);

	void addRoleTo(PermissionOwnerId<?> ownerId, RoleId roleId);
	void deleteRoleFrom(PermissionOwnerId<?> ownerId, RoleId roleId);


	List<User> getUsersByRole(Role role);
	List<User> getAllUsers();
	void addUser(User user);
	void addUsers(List<User> users);
	void deleteUser(UserId userId);

	List<Group> getGroups(Role role);
	Collection<Group> getAllGroups();
	void addGroup(Group group);
	void addGroups(List<Group> groups);
	void removeGroup(GroupId groupId);
	void addUserToGroup(GroupId groupId, UserId userId);
	void deleteUserFromGroup(GroupId groupId, UserId userId);

	
	/**
	 * Handles creation of permissions.
	 *
	 * @param permission
	 *            The permission to create.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	void createPermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission);
	
	/**
	 * Handles deletion of permissions.
	 *
	 * @param permission
	 *            The permission to delete.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	void deletePermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission);
}
