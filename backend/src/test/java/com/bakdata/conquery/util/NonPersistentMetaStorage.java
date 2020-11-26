package com.bakdata.conquery.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.DatasetRegistry;


public class NonPersistentMetaStorage implements MetaStorage {
	
	private static final UnsupportedOperationException NOT_IMPLEMENTED = new UnsupportedOperationException("Not implemented");
	
	private final IdMap<UserId,User> USERS = new IdMap<>();
	private final IdMap<GroupId,Group> GROUPS = new IdMap<>();
	private final IdMap<RoleId,Role> ROLES = new IdMap<>();
	private final Map<ManagedExecutionId,ManagedExecution<?>> EXECUTIONS = new ConcurrentHashMap<>();
	private final IdMap<FormConfigId,FormConfig> FORM_CONFIGS = new IdMap<>();

	@Override
	public Validator getValidator() {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public CentralRegistry getCentralRegistry() {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void loadData() {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void clear() {
		USERS.clear();
		GROUPS.clear();
		ROLES.clear();
		EXECUTIONS.clear();
		FORM_CONFIGS.clear();
	}

	@Override
	public String getStorageOrigin() {
		return "Non-persistent storage just for testing.";
	}

	@Override
	public void close() throws IOException {
		// Nothing to do
	}

	@Override
	public synchronized void addExecution(ManagedExecution<?> query) {
		if(EXECUTIONS.get(query.getId()) != null) {
			throw new IllegalStateException();
		}
		EXECUTIONS.put(query.getId(), query);
	}

	@Override
	public ManagedExecution<?> getExecution(ManagedExecutionId id) {
		return EXECUTIONS.get(id);
	}

	@Override
	public Collection<ManagedExecution<?>> getAllExecutions() {
		return EXECUTIONS.values();
	}

	@Override
	public void updateExecution(ManagedExecution<?> query) throws JSONException {
		EXECUTIONS.put(query.getId(), query);
	}

	@Override
	public void removeExecution(ManagedExecutionId id) {
		EXECUTIONS.remove(id);
	}

	@Override
	public synchronized void addUser(User user) {
		USERS.add(user);
	}

	@Override
	public User getUser(UserId id) {
		return USERS.get(id);
	}

	@Override
	public Collection<User> getAllUsers() {
		return USERS.values();
	}

	@Override
	public void updateUser(User user) {
		USERS.update(user);
	}

	@Override
	public void removeUser(UserId id) {
		USERS.remove(id);
	}

	@Override
	public void addRole(Role role) {
		ROLES.add(role);
	}

	@Override
	public Role getRole(RoleId id) {
		return ROLES.get(id);
	}

	@Override
	public Collection<Role> getAllRoles() {
		return ROLES.values();
	}

	@Override
	public void removeRole(RoleId id) {
		ROLES.remove(id);
	}

	@Override
	public void updateRole(Role role) {
		ROLES.update(role);
	}

	@Override
	public void addGroup(Group group) {
		GROUPS.add(group);
	}

	@Override
	public Group getGroup(GroupId id) {
		return GROUPS.get(id);
	}

	@Override
	public Collection<Group> getAllGroups() {
		return GROUPS.values();
	}

	@Override
	public void removeGroup(GroupId id) {
		GROUPS.remove(id);
	}

	@Override
	public void updateGroup(Group group) {
		GROUPS.update(group);
	}

	@Override
	public DatasetRegistry getDatasetRegistry() {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public FormConfig getFormConfig(FormConfigId id) {
		return FORM_CONFIGS.get(id);
	}

	@Override
	public Collection<FormConfig> getAllFormConfigs() {
		return FORM_CONFIGS.values();
	}

	@Override
	public void removeFormConfig(FormConfigId id) {
		FORM_CONFIGS.remove(id);
	}

	@Override
	public void updateFormConfig(FormConfig config) {
		FORM_CONFIGS.update(config);
	}

	@Override
	public void addFormConfig(FormConfig formConfig) {
		FORM_CONFIGS.add(formConfig);
	}

}
