package com.bakdata.conquery.util;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.ManagedStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

/**
 * A meta storage that can be injected in to deserialization in environments where no MetaStorage exists, e.g. in tests on the client side.
 * During debugging this can help to identify where an object was deserialized.
 */
public class FailingMetaStorage extends MetaStorage {

	public final static FailingMetaStorage INSTANCE = new FailingMetaStorage();
	public static final String ERROR_MSG = "Cannot be used in this environment. The real metastore exists only on the manager node.";

	private FailingMetaStorage() {
		super(null);
	}

	@Override
	public void openStores(ObjectMapper mapper) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public void addExecution(ManagedExecution query) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public ManagedExecution getExecution(ManagedExecutionId id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Stream<ManagedExecution> getAllExecutions() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void updateExecution(ManagedExecution query) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void removeExecution(ManagedExecutionId id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void addGroup(Group group) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Group getGroup(GroupId groupId) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Stream<Group> getAllGroups() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public void removeGroup(GroupId id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void updateGroup(Group group) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void addUser(User user) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public User getUser(UserId userId) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Stream<User> getAllUsers() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void removeUser(UserId userId) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void updateUser(User user) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void addRole(Role role) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Role getRole(RoleId roleId) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Stream<Role> getAllRoles() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void removeRole(RoleId roleId) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void updateRole(Role role) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public FormConfig getFormConfig(FormConfigId id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public Stream<FormConfig> getAllFormConfigs() {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void removeFormConfig(FormConfigId id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void updateFormConfig(FormConfig formConfig) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public synchronized void addFormConfig(FormConfig formConfig) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	public <ID extends MetaId<?>, VALUE> VALUE get(ID id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}
}
