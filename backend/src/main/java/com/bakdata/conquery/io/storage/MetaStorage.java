package com.bakdata.conquery.io.storage;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
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
	protected final DatasetRegistry datasetRegistry;
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
	public ImmutableList<KeyIncludingStore<?, ?>> getStores() {
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

	public void addUser(User user) {
		authUser.add(user);
	}

	public User getUser(UserId userId) {
		return authUser.get(userId);
	}

	public Collection<User> getAllUsers() {
		return authUser.getAll();
	}

	public void removeUser(UserId userId) {
		log.info("Remove User = {}", userId);
		authUser.remove(userId);
	}

	public void addRole(Role role) {
		authRole.add(role);
	}

	public Role getRole(RoleId roleId) {
		return authRole.get(roleId);
	}

	public Collection<Role> getAllRoles() {
		return authRole.getAll();
	}

	public void removeRole(RoleId roleId) {
		authRole.remove(roleId);
	}

	public void updateUser(User user) {
		authUser.update(user);
	}

	public void updateRole(Role role) {
		authRole.update(role);
	}

	public void addGroup(Group group) {
		authGroup.add(group);
	}

	public Group getGroup(GroupId id) {
		return authGroup.get(id);
	}

	public Collection<Group> getAllGroups() {
		return authGroup.getAll();
	}

	public void removeGroup(GroupId id) {
		authGroup.remove(id);
	}

	public void updateGroup(Group group) {
		authGroup.update(group);
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
