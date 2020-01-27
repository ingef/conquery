package com.bakdata.conquery.models.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.functions.ThrowingRunnable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalAuthStorage implements AuthorizationStorage{
	private final static Function<PermissionOwnerId<?>, UnsupportedOperationException> UNSUPPORTED_TYPE = (id) -> new UnsupportedOperationException(String.format("The type of %s (%s) is not supported", id, id.getClass().getName()));
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
	
	@Getter
	private String id;
	
	
	public LocalAuthStorage(StorageConfig config, Validator validator, CentralRegistry registry) {
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
		
		authRole = StoreInfo.AUTH_ROLE.identifiable(getRolesEnvironment(), validator, registry);

		authUser = StoreInfo.AUTH_USER.identifiable(getUsersEnvironment(), validator, registry);

		authGroup = StoreInfo.AUTH_GROUP.identifiable(getUsersEnvironment(), validator, registry);
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

	public void removeRole(RoleId roleId) {
		log.info("Deleting mandator: {}", roleId);
		Role role = authRole.get(roleId);
		for (User user : authUser.getAll()) {
			asRuntimeException(() -> {
				user.removeRole(this, role);
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

	public List<Group> getGroupsByRole(Role role) {
		return authGroup.getAll().stream().filter(g -> g.getRoles().contains(role)).collect(Collectors.toList());
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(authUser.getAll());
	}

	public void deleteUser(UserId userId) {
		User user = authUser.get(userId);
		for (Group group : getAllGroups()) {
			asRuntimeException(() -> {
				group.removeMember(this, user);
			});
		}
		authUser.remove(userId);
		log.trace("Removed user {} from the storage.", userId);
	}

	public void addUser(User user){
		asRuntimeException(() -> {
			ValidatorHelper.failOnError(log, validator.validate(user));
			authUser.add(user);
		});
		log.trace("New user:\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
	}

	public void addUsers(List<User> users) {
		Objects.requireNonNull(users, "User list was empty.");
		for (User user : users) {
			addUser(user);
		}
	}

	public Collection<Group> getAllGroups() {
		return authGroup.getAll();
	}

	public void addGroup(Group group){
		asRuntimeException(() -> {
			ValidatorHelper.failOnError(log, validator.validate(group));
			authGroup.add(group);
		});
		log.trace("New group:\tLabel: {}\tName: {}\tId: {} ", group.getLabel(), group.getName(), group.getId());

	}

	public void addGroups(List<Group> groups) {
		for (Group group : groups) {
			addGroup(group);
		}
	}

	public void removeGroup(GroupId groupId) {
		authGroup.remove(groupId);
		log.trace("Removed group {}", groupId);
	}
	
	public static void asRuntimeException(ThrowingRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot validate or store the given Argument.", e);
		}
		
	}

	@Override
	public User getUser(UserId userId) {
		return authUser.get(userId);
	}
	

	@Override
	public Role getRole(RoleId roleId) {
		return authRole.get(roleId);
	}

	@Override
	public void updateUser(User user) {
		asRuntimeException(() -> authUser.update(user));
	}

	@Override
	public void updateRole(Role role) {
		asRuntimeException(() -> authRole.update(role));
	}

	@Override
	public void updateGroup(Group group) {
		asRuntimeException(() -> authGroup.update(group));
	}

	@Override
	public Group getGroup(GroupId groupId) {
		return authGroup.get(groupId);
	}
}
