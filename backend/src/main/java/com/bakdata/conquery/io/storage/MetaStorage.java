package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MetaStorage extends ConqueryStorage implements Injectable {

	private final StoreFactory storageFactory;
	private IdentifiableStore<ManagedExecution> executions;
	private IdentifiableStore<FormConfig> formConfigs;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	private LoadingCache<Id<?>, Identifiable<?>> cache;

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

		cache = Caffeine.from(storageFactory.getCacheSpec())
						.build(this::<Id, Identifiable<?>>getFromStorage);

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

	@Override
	public void clear() {
		super.clear();
	}

	public void addExecution(ManagedExecution query) {
		executions.add(query);
	}

	public ManagedExecution getExecution(ManagedExecutionId id) {
		return executions.get(id);
	}

	public Stream<ManagedExecution> getAllExecutions() {
		return executions.getAll();
	}

	public synchronized void updateExecution(ManagedExecution query) {
		cache.invalidate(query.getId());
		executions.update(query);
	}

	public synchronized void removeExecution(ManagedExecutionId id) {
		cache.invalidate(id);
		executions.remove(id);
	}

	public synchronized void addGroup(Group group) {
		cache.invalidate(group.getId());
		log.info("Adding group = {}", group.getId());
		authGroup.add(group);
	}

	public Group getGroup(GroupId groupId) {
		final Group group = authGroup.get(groupId);
		log.trace("Requested group '{}' got: {}", groupId, group);
		return group;
	}

	public Stream<Group> getAllGroups() {
		return authGroup.getAll();
	}

	public void removeGroup(GroupId id) {
		cache.invalidate(id);
		log.info("Removing group = {}", id);
		authGroup.remove(id);
	}

	public synchronized void updateGroup(Group group) {
		cache.invalidate(group.getId());
		log.info("Updating group = {}", group.getId());
		authGroup.update(group);
	}

	public synchronized void addUser(User user) {
		cache.invalidate(user.getId());
		log.info("Adding user = {}", user.getId());
		authUser.add(user);
	}

	public User getUser(UserId userId) {
		final User user = authUser.get(userId);
		log.trace("Requested user '{}' got: {}", userId, user);
		return user;
	}

	public Stream<User> getAllUsers() {
		return authUser.getAll();
	}

	public synchronized void removeUser(UserId userId) {
		cache.invalidate(userId);
		log.info("Removing user = {}", userId);
		authUser.remove(userId);
	}

	public synchronized void updateUser(User user) {
		cache.invalidate(user.getId());
		log.info("Updating user = {}", user.getId());
		authUser.update(user);
	}

	public synchronized void addRole(Role role) {
		cache.invalidate(role.getId());
		authRole.add(role);
	}

	public Role getRole(RoleId roleId) {
		final Role role = authRole.get(roleId);
		log.trace("Requested role '{}' got: {}", roleId, role);
		return role;
	}

	public Stream<Role> getAllRoles() {
		return authRole.getAll();
	}

	public synchronized void removeRole(RoleId roleId) {
		cache.invalidate(roleId);
		log.info("Removing role = {}", roleId);
		authRole.remove(roleId);
	}

	public synchronized void updateRole(Role role) {
		cache.invalidate(role.getId());
		log.info("Updating role = {}", role.getId());
		authRole.update(role);
	}

	public FormConfig getFormConfig(FormConfigId id) {
		return formConfigs.get(id);
	}

	public Stream<FormConfig> getAllFormConfigs() {
		return formConfigs.getAll();
	}

	public synchronized void removeFormConfig(FormConfigId id) {
		cache.invalidate(id);
		formConfigs.remove(id);
	}

	@SneakyThrows
	public synchronized void updateFormConfig(FormConfig formConfig) {
		cache.invalidate(formConfig.getId());
		formConfigs.update(formConfig);
	}

	@SneakyThrows
	public synchronized void addFormConfig(FormConfig formConfig) {
		cache.invalidate(formConfig.getId());
		formConfigs.add(formConfig);
	}


	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MetaStorage.class, this);
	}

	public <ID extends Id<?>, VALUE> VALUE get(ID id) {
		return (VALUE) cache.get(id);
	}

	protected <ID extends Id<VALUE>, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof ManagedExecutionId executionId) {
			return (VALUE) getExecution(executionId);
		}
		if (id instanceof FormConfigId formConfigId) {
			return (VALUE) getFormConfig(formConfigId);
		}
		if (id instanceof GroupId groupId) {
			return (VALUE) getGroup(groupId);
		}
		if (id instanceof RoleId roleId) {
			return (VALUE) getRole(roleId);
		}
		if (id instanceof UserId userId) {
			return (VALUE) getUser(userId);
		}

		throw new IllegalArgumentException("Id type '" + id.getClass() + "' is not supported");
	}

	public static MetaStorage get(DeserializationContext ctxt) throws JsonMappingException {
		return (MetaStorage) ctxt
				.findInjectableValue(MetaStorage.class.getName(), null, null);
	}
}
