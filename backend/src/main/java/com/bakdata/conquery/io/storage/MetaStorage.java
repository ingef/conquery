package com.bakdata.conquery.io.storage;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MetaStorage extends ConqueryStorage implements Injectable {

	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
	private final StoreFactory storageFactory;

	@Getter
	protected final DatasetRegistry<? extends Namespace> datasetRegistry;
	private IdentifiableStore<ManagedExecution> executions;
	private IdentifiableStore<FormConfig> formConfigs;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	public void openStores(ObjectMapper mapper) {
		authUser = storageFactory.createUserStore(centralRegistry, "meta", this, mapper);
		authRole = storageFactory.createRoleStore(centralRegistry, "meta", this, mapper);
		authGroup = storageFactory.createGroupStore(centralRegistry, "meta", this, mapper);
		// Executions depend on users
		executions = storageFactory.createExecutionsStore(centralRegistry, datasetRegistry, "meta", mapper);
		formConfigs = storageFactory.createFormConfigStore(centralRegistry, datasetRegistry, "meta", mapper);

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
		centralRegistry.clear();
	}

	public void addExecution(ManagedExecution query) {
		executions.add(query);
	}

	public ManagedExecution getExecution(ManagedExecutionId id) {
		return executions.get(id);
	}

	public Collection<ManagedExecution> getAllExecutions() {
		return executions.getAll();
	}

	public void updateExecution(ManagedExecution query) {
		executions.update(query);
	}

	public void removeExecution(ManagedExecutionId id) {
		executions.remove(id);
	}

	public void addGroup(Group group) {
		log.info("Adding group = {}", group.getId());
		authGroup.add(group);
	}

	public Group getGroup(GroupId groupId) {
		final Group group = authGroup.get(groupId);
		log.trace("Requested group '{}' got: {}", groupId, group);
		return group;
	}

	public Collection<Group> getAllGroups() {
		return authGroup.getAll();
	}

	public void removeGroup(GroupId id) {
		log.info("Removing group = {}", id);
		authGroup.remove(id);
	}

	public void updateGroup(Group group) {
		log.info("Updating group = {}", group.getId());
		authGroup.update(group);
	}

	public void addUser(User user) {
		log.info("Adding user = {}", user.getId());
		authUser.add(user);
	}

	public User getUser(UserId userId) {
		final User user = authUser.get(userId);
		log.trace("Requested user '{}' got: {}", userId, user);
		return user;
	}

	public Collection<User> getAllUsers() {
		return authUser.getAll();
	}

	public void removeUser(UserId userId) {
		log.info("Removing user = {}", userId);
		authUser.remove(userId);
	}

	public void updateUser(User user) {
		log.info("Updating user = {}", user.getId());
		authUser.update(user);
	}

	public void addRole(Role role) {
		authRole.add(role);
	}

	public Role getRole(RoleId roleId) {
		final Role role = authRole.get(roleId);
		log.trace("Requested role '{}' got: {}", roleId, role);
		return role;
	}

	public Collection<Role> getAllRoles() {
		return authRole.getAll();
	}

	public void removeRole(RoleId roleId) {
		log.info("Removing role = {}", roleId);
		authRole.remove(roleId);
	}

	public void updateRole(Role role) {
		log.info("Updating role = {}", role.getId());
		authRole.update(role);
	}

	public FormConfig getFormConfig(FormConfigId id) {
		return formConfigs.get(id);
	}

	public Collection<FormConfig> getAllFormConfigs() {
		return formConfigs.getAll();
	}

	public void removeFormConfig(FormConfigId id) {
		formConfigs.remove(id);
	}

	@SneakyThrows
	public void updateFormConfig(FormConfig formConfig) {
		formConfigs.update(formConfig);
	}

	@SneakyThrows
	public void addFormConfig(FormConfig formConfig) {
		formConfigs.add(formConfig);
	}


	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MetaStorage.class, this);
	}
}
