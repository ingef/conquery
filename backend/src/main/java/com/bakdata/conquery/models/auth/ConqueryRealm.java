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
import org.apache.shiro.realm.AuthorizingRealm;

public abstract class ConqueryRealm  extends AuthorizingRealm {
	abstract void addRole(Role role);

	abstract void addRoles(List<Role> roles);

	/**
	 * Deletes the mandator, that is identified by the id. Its references are
	 * removed from the users and from the storage.
	 *
	 * @param mandatorId
	 *            The id belonging to the mandator
	 * @throws JSONException
	 *             is thrown on JSON validation form the storage.
	 */
	abstract void deleteRole(RoleId mandatorId);

	abstract List<Role> getAllRoles();

	abstract List<User> getUsers(Role role);

	abstract List<Group> getGroups(Role role);

	/**
	 * Handles creation of permissions.
	 *
	 * @param permission
	 *            The permission to create.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	abstract void createPermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission);

	/**
	 * Handles deletion of permissions.
	 *
	 * @param permission
	 *            The permission to delete.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	abstract void deletePermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission);

	abstract List<User> getAllUsers();

	abstract void deleteUser(UserId userId);

	abstract void addUser(User user);

	abstract void addUsers(List<User> users);

	abstract Collection<Group> getAllGroups();
	
	abstract void addGroup(Group group);

	abstract void addGroups(List<Group> groups);

	abstract void addUserToGroup(GroupId groupId, UserId userId);

	abstract void deleteUserFromGroup(GroupId groupId, UserId userId);

	abstract void removeGroup(GroupId groupId);

	abstract void deleteRoleFrom(PermissionOwnerId<?> ownerId, RoleId roleId);

	abstract void addRoleTo(PermissionOwnerId<?> ownerId, RoleId roleId);

}
