package com.bakdata.conquery.io.storage;

import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Access to persisted entities that are not namespace/dataset crucial (see {@link NamespacedStorageImpl}).
 * All entities are loaded through a cache. The cache can be configured through the StoreFactory.
 */
@Slf4j
@RequiredArgsConstructor
public class MetaStorage implements ConqueryStorage, Injectable {

	private final StoreFactory storageFactory;
	private IdentifiableStore<ManagedExecution> executions;
	private IdentifiableStore<FormConfig> formConfigs;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	public static MetaStorage getInjected(DeserializationContext ctxt) throws JsonMappingException {
		return (MetaStorage) ctxt
				.findInjectableValue(MetaStorage.class.getName(), null, null);
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		Preconditions.checkNotNull(authUser, "User storage was not created");
		Preconditions.checkNotNull(authRole, "Role storage was not created");
		Preconditions.checkNotNull(authGroup, "Group storage was not created");
		Preconditions.checkNotNull(executions, "Execution storage was not created");
		Preconditions.checkNotNull(formConfigs, "FormConfig storage was not created");

		return ImmutableList.of(
				authUser,
				authRole,
				authGroup,
				executions,
				formConfigs
		);
	}

	public void openStores(ObjectMapper mapper) {
		if (mapper != null) {
			this.injectInto(mapper);
		}
		authUser = storageFactory.createUserStore("meta", mapper);
		authRole = storageFactory.createRoleStore("meta", mapper);
		authGroup = storageFactory.createGroupStore("meta", mapper);
		// Executions depend on users
		executions = storageFactory.createExecutionsStore("meta", mapper);
		formConfigs = storageFactory.createFormConfigStore("meta", mapper);

	}

	// Executions

	public void addExecution(ManagedExecution query) {
		executions.add(query);
	}

	public ManagedExecution getExecution(ManagedExecutionId id) {
		return executions.get(id);
	}

	public Stream<ManagedExecution> getAllExecutions() {
		return executions.getAllKeys()
						 .map(executions::get)
						 .filter(Objects::nonNull);
	}

	public synchronized void updateExecution(ManagedExecution query) {
		executions.update(query);
	}

	public synchronized void removeExecution(ManagedExecutionId id) {
		executions.remove(id);
	}

	// Groups

	public synchronized void addGroup(Group group) {
		log.info("Adding group = {}", group.getId());
		authGroup.add(group);
	}

	public Group getGroup(GroupId groupId) {
		final Group group = authGroup.get(groupId);
		log.trace("Requested group '{}' got: {}", groupId, group);
		return group;
	}

	public Stream<Group> getAllGroups() {
		return authGroup.getAllKeys().map(authGroup::get);
	}

	public void removeGroup(GroupId id) {
		log.info("Removing group = {}", id);
		authGroup.remove(id);
	}

	public synchronized void updateGroup(Group group) {
		log.info("Updating group = {}", group.getId());
		authGroup.update(group);
	}

	// User

	public synchronized void addUser(User user) {
		log.info("Adding user = {}", user.getId());
		authUser.add(user);
	}

	public User getUser(UserId userId) {
		final User user = authUser.get(userId);
		log.trace("Requested user '{}' got: {}", userId, user);
		return user;
	}

	public Stream<User> getAllUsers() {
		return authUser.getAllKeys().map(authUser::get);
	}

	public synchronized void removeUser(UserId userId) {
		log.info("Removing user = {}", userId);
		authUser.remove(userId);
	}

	public synchronized void updateUser(User user) {
		log.info("Updating user = {}", user.getId());
		authUser.update(user);
	}

	// Roles

	public synchronized void addRole(Role role) {
		authRole.add(role);
	}

	public Role getRole(RoleId roleId) {
		final Role role = authRole.get(roleId);
		log.trace("Requested role '{}' got: {}", roleId, role);
		return role;
	}

	public Stream<Role> getAllRoles() {
		return authRole.getAllKeys().map(authRole::get);
	}

	public synchronized void removeRole(RoleId roleId) {
		log.info("Removing role = {}", roleId);
		authRole.remove(roleId);
	}

	public synchronized void updateRole(Role role) {
		log.info("Updating role = {}", role.getId());
		authRole.update(role);
	}

	// FormConfigs

	public FormConfig getFormConfig(FormConfigId id) {
		return formConfigs.get(id);
	}

	public Stream<FormConfig> getAllFormConfigs() {
		return formConfigs.getAllKeys().map(formConfigs::get);
	}

	public synchronized void removeFormConfig(FormConfigId id) {
		formConfigs.remove(id);
	}

	@SneakyThrows
	public synchronized void updateFormConfig(FormConfig formConfig) {
		formConfigs.update(formConfig);
	}

	@SneakyThrows
	public synchronized void addFormConfig(FormConfig formConfig) {
		formConfigs.add(formConfig);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MetaStorage.class, this);
	}

	public <ID extends MetaId<?>, VALUE> VALUE get(ID id) {
		return (VALUE) id.get(this);
	}
}
