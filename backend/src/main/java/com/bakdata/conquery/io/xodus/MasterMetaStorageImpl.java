package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfigInternal;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.functions.Collector;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link MasterMetaStorage} using {@link XodusStore}s
 * under the hood. All elements are stored as binary JSON in the Smile format.
 * The {@link JSONException}s that can be thrown by this serdes process are sneakily
 * converted into {@link RuntimeException}s by this implementation.
 */
@Slf4j
public class MasterMetaStorageImpl extends ConqueryStorageImpl implements MasterMetaStorage, ConqueryStorage {

	private SingletonStore<Namespaces> meta;
	private IdentifiableStore<ManagedExecution<?>> executions;
	private IdentifiableStore<FormConfigInternal> formConfigs;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	@Getter
	private Namespaces namespaces;

	@Getter
	private final Environment executionsEnvironment;

	@Getter
	private final Environment formConfigEnvironment;

	@Getter
	private final Environment usersEnvironment;

	@Getter
	private final Environment rolesEnvironment;

	@Getter
	private final Environment groupsEnvironment;

	public MasterMetaStorageImpl(Namespaces namespaces, Validator validator, StorageConfig config) {
		super(validator, config, new File(config.getDirectory(), "meta"));

		executionsEnvironment = Environments.newInstance(new File(config.getDirectory(), "executions"), config.getXodus().createConfig());

		formConfigEnvironment = Environments.newInstance(new File(config.getDirectory(), "formConfigs"), config.getXodus().createConfig());

		usersEnvironment = Environments.newInstance(new File(config.getDirectory(), "users"), config.getXodus().createConfig());

		rolesEnvironment = Environments.newInstance(new File(config.getDirectory(), "roles"), config.getXodus().createConfig());

		groupsEnvironment = Environments.newInstance(new File(config.getDirectory(), "groups"), config.getXodus().createConfig());

		this.namespaces = namespaces;
	}

	@Override
	protected void createStores(Collector<KeyIncludingStore<?, ?>> collector) {

		meta = StoreInfo.NAMESPACES.singleton(getEnvironment(), getValidator());

		executions = StoreInfo.EXECUTIONS
			.<ManagedExecution<?>>identifiable(getExecutionsEnvironment(), getValidator(), getCentralRegistry(), namespaces);
		authRole = StoreInfo.AUTH_ROLE.identifiable(getRolesEnvironment(), getValidator(), getCentralRegistry());

		authUser = StoreInfo.AUTH_USER.identifiable(getUsersEnvironment(), getValidator(), getCentralRegistry());

		authGroup = StoreInfo.AUTH_GROUP.identifiable(getGroupsEnvironment(), getValidator(), getCentralRegistry());
		
		formConfigs = StoreInfo.FORM_CONFIG.identifiable(getFormConfigEnvironment(), getValidator(), getCentralRegistry());

		collector
			.collect(meta)
			.collect(authRole)
			// load users before queries
			.collect(authUser)
			.collect(authGroup)
			.collect(executions)
			.collect(formConfigs);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addExecution(ManagedExecution query) {
		executions.add(query);
	}

	@Override
	public ManagedExecution<?> getExecution(ManagedExecutionId id) {
		return executions.get(id);
	}

	@Override
	public Collection<ManagedExecution<?>> getAllExecutions() {
		return executions.getAll();
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateExecution(ManagedExecution<?> query) {
		executions.update(query);
	}

	@Override
	public void removeExecution(ManagedExecutionId id) {
		executions.remove(id);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addUser(User user) {
		authUser.add(user);
	}

	@Override
	public User getUser(UserId userId) {
		return authUser.get(userId);
	}

	@Override
	public Collection<User> getAllUsers() {
		return authUser.getAll();
	}

	@Override
	public void removeUser(UserId userId) {
		authUser.remove(userId);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addRole(Role role) {
		authRole.add(role);
	}

	@Override
	public Role getRole(RoleId roleId) {
		return authRole.get(roleId);
	}

	@Override
	public Collection<Role> getAllRoles() {
		return authRole.getAll();
	}

	@Override
	public void removeRole(RoleId roleId) {
		authRole.remove(roleId);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateUser(User user) {
		authUser.update(user);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateRole(Role role) {
		authRole.update(role);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addGroup(Group group) {
		authGroup.add(group);
	}

	@Override
	public Group getGroup(GroupId id) {
		return authGroup.get(id);
	}

	@Override
	public Collection<Group> getAllGroups() {
		return authGroup.getAll();
	}

	@Override
	public void removeGroup(GroupId id) {
		authGroup.remove(id);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateGroup(Group group) {
		authGroup.update(group);
	}

	@Override
	public FormConfigInternal getFormConfig(FormConfigId id) {
		return formConfigs.get(id);
	}

	@Override
	public Collection<FormConfigInternal> getAllFormConfigs() {
		return formConfigs.getAll();
	}

	@Override
	public void removeFormConfig(FormConfigId id) {
		formConfigs.remove(id);
	}

	@Override
	@SneakyThrows
	public void updateFormConfig(FormConfigInternal formConfig) {
		formConfigs.update(formConfig);
	}
	
	@Override
	@SneakyThrows
	public void addFormConfig(FormConfigInternal formConfig) {
		formConfigs.add(formConfig);
	}
	
	@Override
	public void close() throws IOException {
		getExecutionsEnvironment().close();
		getFormConfigEnvironment().close();
		getGroupsEnvironment().close();
		getUsersEnvironment().close();
		getRolesEnvironment().close();
		
		super.close();
	}
}
