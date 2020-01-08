package com.bakdata.conquery.models.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.RoleOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.ui.model.FEGroupContent;
import com.bakdata.conquery.util.functions.ThrowingRunnable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalAuthStorage implements AuthStorage{
	private final Validator validator;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	@Getter
	private final Environment usersEnvironment;

	@Getter
	private final Environment rolesEnvironment;

	@Getter
	private final Environment groupsEnvironment;
	
	
	public LocalAuthStorage(StorageConfig config, Validator validator) {
		this.validator = validator;


		usersEnvironment = Environments.newInstance(
				new File(config.getDirectory(), "users"),
				config.getXodus().createConfig()
		);

		rolesEnvironment = Environments.newInstance(
				new File(config.getDirectory(), "roles"),
				config.getXodus().createConfig()
		);

		groupsEnvironment = Environments.newInstance(
			new File(config.getDirectory(), "groups"),
			config.getXodus().createConfig()
			);
	}
	
	public void addRole(Role role){
		try {
			ValidatorHelper.failOnError(log, validator.validate(role));
			log.trace("New role:\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
			authRole.add(role);
		}
		catch (JSONException e) {
			throw new IllegalArgumentException("Validation or storing failed for " + role, e);
		}
	}

	public void addRoles(List<Role> roles) {
		for (Role role : roles) {
			addRole(role);
		}
	}

	public void deleteRole(RoleId roleId) {
		log.info("Deleting mandator: {}", roleId);
		Role role = authRole.get(roleId);
		for (User user : authUser.getAll()) {
			asRuntimeException(() -> {
				user.removeRole(role);
				authUser.update(user);
			});
		}
		authRole.remove(roleId);
	}

	public List<Role> getAllRoles() {
		return new ArrayList<>(authRole.getAll());
	}

	public List<User> getUsersByRole(Role role) {
		return authUser.getAll().stream().filter(u -> u.getRoles().contains(role)).collect(Collectors.toList());
	}

	private List<Group> getGroupsByRole(Role role) {
		return authGroup.getAll().stream().filter(g -> g.getRoles().contains(role)).collect(Collectors.toList());
	}


	public void createPermission(UserId ownerId, ConqueryPermission permission) {
		createPermission(authUser, ownerId, permission);
	}
	
	public void deletePermission(UserId ownerId, ConqueryPermission permission) throws JSONException {
		deletePermission(authUser, ownerId, permission);
	}
	
	public void createPermission(RoleId ownerId, ConqueryPermission permission) {
		createPermission(authRole, ownerId, permission);
	}
	
	public void deletePermission(RoleId ownerId, ConqueryPermission permission) throws JSONException {
		deletePermission(authRole, ownerId, permission);
	}
	
	public void createPermission(GroupId ownerId, ConqueryPermission permission) {
		createPermission(authGroup, ownerId, permission);
	}
	
	public void deletePermission(GroupId ownerId, ConqueryPermission permission) throws JSONException {
		deletePermission(authGroup, ownerId, permission);
	}
	
	private static <P extends PermissionOwner<?>> void createPermission(IdentifiableStore<P> store, PermissionOwnerId<P> ownerId, ConqueryPermission permission) {
		P owner = store.get(ownerId);
		asRuntimeException(() -> {
			owner.addPermission(permission);
			store.update(owner);
		});
	}
	
	private static <P extends PermissionOwner<?>> void deletePermission(IdentifiableStore<P> store, PermissionOwnerId<P> ownerId, ConqueryPermission permission) {
		P owner = store.get(ownerId);
		asRuntimeException(() -> {
			owner.removePermission(permission);
			store.update(owner);
		});
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(authUser.getAll());
	}

	public synchronized void deleteUser(UserId userId) {
		storage.removeUser(userId);
		log.trace("Removed user {} from the storage.", userId);
	}

	public synchronized void addUser(User user) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(user));
		storage.addUser(user);
		log.trace("New user:\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
	}

	public void addUsers(List<User> users) {
		Objects.requireNonNull(users, "User list was empty.");
		for (User user : users) {
			try {
				addUser(user);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add User: %s", user), e);
			}
		}
	}

	public Collection<Group> getAllGroups() {
		return storage.getAllGroups();
	}

	public FEGroupContent getGroupContent(GroupId groupId) {
		Group group = Objects.requireNonNull(storage.getGroup(groupId));
		Set<User> members = group.getMembers();
		ArrayList<User> availableMembers = new ArrayList<>(storage.getAllUsers());
		availableMembers.removeAll(members);
		return FEGroupContent
			.builder()
			.owner(group)
			.members(members)
			.availableMembers(availableMembers)
			.roles(group.getRoles())
			.availableRoles(storage.getAllRoles())
			.permissions(wrapInFEPermission(group.getPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.build();
	}

	public synchronized void addGroup(Group group) throws JSONException {
		synchronized (storage) {
			ValidatorHelper.failOnError(log, validator.validate(group));
			storage.addGroup(group);
		}
		log.trace("New group:\tLabel: {}\tName: {}\tId: {} ", group.getLabel(), group.getName(), group.getId());

	}

	public void addGroups(List<Group> groups) {
		Objects.requireNonNull(groups, "Group list was null.");
		for (Group group : groups) {
			try {
				addGroup(group);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Group: %s", group), e);
			}
		}
	}

	public void addUserToGroup(GroupId groupId, UserId userId) throws JSONException {
		synchronized (storage) {
			Objects
				.requireNonNull(groupId.getPermissionOwner(storage))
				.addMember(storage, Objects.requireNonNull(userId.getPermissionOwner(storage)));
		}
		log.trace("Added user {} to group {}", userId.getPermissionOwner(storage), groupId.getPermissionOwner(getStorage()));
	}

	public void deleteUserFromGroup(GroupId groupId, UserId userId) throws JSONException {
		synchronized (storage) {
			Objects
				.requireNonNull(groupId.getPermissionOwner(storage))
				.removeMember(storage, Objects.requireNonNull(userId.getPermissionOwner(storage)));
		}
		log.trace("Removed user {} from group {}", userId.getPermissionOwner(storage), groupId.getPermissionOwner(getStorage()));
	}

	public void removeGroup(GroupId groupId) {
		synchronized (storage) {
			storage.removeGroup(groupId);
		}
		log.trace("Removed group {}", groupId.getPermissionOwner(getStorage()));
	}

	public void deleteRoleFrom(PermissionOwnerId<?> ownerId, RoleId roleId) throws JSONException {
		PermissionOwner<?> owner = null;
		Role role = null;
		synchronized (storage) {
			owner = Objects.requireNonNull(ownerId.getPermissionOwner(storage));
			role = Objects.requireNonNull(storage.getRole(roleId));
		}
		if (!(owner instanceof RoleOwner)) {
			throw new IllegalStateException(String.format("Provided entity %s cannot hold any roles", owner));
		}
		((RoleOwner) owner).removeRole(storage, role);
		log.trace("Deleted role {} from {}", role, owner);
	}

	public void addRoleTo(PermissionOwnerId<?> ownerId, RoleId roleId) throws JSONException {
		PermissionOwner<?> owner = null;
		Role role = null;
		synchronized (storage) {
			owner = Objects.requireNonNull(ownerId.getPermissionOwner(storage));
			role = Objects.requireNonNull(storage.getRole(roleId));
		}
		if (!(owner instanceof RoleOwner)) {
			throw new IllegalStateException(String.format("Provided entity %s cannot hold any roles", owner));
		}
		((RoleOwner) owner).addRole(storage, role);
		log.trace("Deleted role {} from {}", role, owner);
	}
	
	public static void asRuntimeException(ThrowingRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot validate or store the given Argument.", e);
		}
		
	}
}
