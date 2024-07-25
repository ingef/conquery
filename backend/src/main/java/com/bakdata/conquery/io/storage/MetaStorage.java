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
import com.bakdata.conquery.models.identifiable.ids.IdResolvingException;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.caffeine.MetricsStatsCounter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
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

/**
 * Access to persisted entities that are not namespace/dataset crucial (see {@link NamespacedStorage}).
 * All entities are loaded through a cache. The cache can be configured through the StoreFactory.
 */
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

	public static MetaStorage get(DeserializationContext ctxt) throws JsonMappingException {
		return (MetaStorage) ctxt
				.findInjectableValue(MetaStorage.class.getName(), null, null);
	}

	public void openStores(ObjectMapper mapper, MetricRegistry metricRegistry) {
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
						.recordStats(() -> new MetricsStatsCounter(metricRegistry, "meta-storage-cache"))
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

	// Executions

	public void addExecution(ManagedExecution query) {
		executions.add(query);
	}

	public ManagedExecution getExecution(ManagedExecutionId id) {
		return get(id);
	}

	private ManagedExecution getExecutionFromStorage(ManagedExecutionId id) {
		return executions.get(id);
	}

	public Stream<ManagedExecution> getAllExecutions() {
		return executions.getAllKeys().map(this::get);
	}

	public synchronized void updateExecution(ManagedExecution query) {
		cache.invalidate(query.getId());
		executions.update(query);
	}

	public synchronized void removeExecution(ManagedExecutionId id) {
		cache.invalidate(id);
		executions.remove(id);
	}

	// Groups

	public synchronized void addGroup(Group group) {
		cache.invalidate(group.getId());
		log.info("Adding group = {}", group.getId());
		authGroup.add(group);
	}

	public Group getGroup(GroupId groupId) {
		final Group group = get(groupId);
		log.trace("Requested group '{}' got: {}", groupId, group);
		return group;
	}

	private Group getGroupFromStorage(GroupId groupId) {
		return authGroup.get(groupId);
	}

	public Stream<Group> getAllGroups() {
		return authGroup.getAllKeys().map(this::get);
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

	// User

	public synchronized void addUser(User user) {
		cache.invalidate(user.getId());
		log.info("Adding user = {}", user.getId());
		authUser.add(user);
	}

	public User getUser(UserId userId) {
		final User user = get(userId);
		log.trace("Requested user '{}' got: {}", userId, user);
		return user;
	}

	private User getUserFromStorage(UserId userId) {
		return authUser.get(userId);
	}

	public Stream<User> getAllUsers() {
		return authUser.getAllKeys().map(this::get);
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

	// Roles

	public synchronized void addRole(Role role) {
		cache.invalidate(role.getId());
		authRole.add(role);
	}

	public Role getRole(RoleId roleId) {
		final Role role = get(roleId);
		log.trace("Requested role '{}' got: {}", roleId, role);
		return role;
	}

	public Role getRoleFromStorage(RoleId roleId) {
		return authRole.get(roleId);
	}

	public Stream<Role> getAllRoles() {
		return authRole.getAllKeys().map(this::get);
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

	// FormConfigs

	public FormConfig getFormConfig(FormConfigId id) {
		return get(id);
	}

	private FormConfig getFormConfigFromStorage(FormConfigId id) {
		return formConfigs.get(id);
	}


	public Stream<FormConfigId> getAllFormConfigIds() {
		return formConfigs.getAllKeys().map(FormConfigId.class::cast);
	}

	public Stream<FormConfig> getAllFormConfigs() {
		return formConfigs.getAllKeys().map(this::get);
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

	// Utility

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MetaStorage.class, this);
	}

	public <ID extends Id<?>, VALUE> VALUE get(ID id) {
		return (VALUE) cache.get(id);
	}

	/**
	 * Almost identical to {@link MetaStorage#get(Id)}, but throws an IdResolvingException if no object could be resolved.
	 * @return the object or throws an {@link IdResolvingException} if the Object could not be resolved.
	 */
	public <ID extends Id<?>, VALUE> VALUE resolve(ID id) {
		try {
			VALUE o = get(id);
			if (o == null) {
				throw new IdResolvingException(id);
			}
			return o;
		}
		catch (Exception e) {
			throw new IdResolvingException(id, e);
		}
	}

	protected <ID extends Id<VALUE>, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof ManagedExecutionId executionId) {
			return (VALUE) getExecutionFromStorage(executionId);
		}
		if (id instanceof FormConfigId formConfigId) {
			return (VALUE) getFormConfigFromStorage(formConfigId);
		}
		if (id instanceof GroupId groupId) {
			return (VALUE) getGroupFromStorage(groupId);
		}
		if (id instanceof RoleId roleId) {
			return (VALUE) getRoleFromStorage(roleId);
		}
		if (id instanceof UserId userId) {
			return (VALUE) getUserFromStorage(userId);
		}

		throw new IllegalArgumentException("Id type '" + id.getClass() + "' is not supported");
	}
}
