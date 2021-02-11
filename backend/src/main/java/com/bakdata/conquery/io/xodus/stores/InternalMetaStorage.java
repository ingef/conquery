package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.validation.Validator;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class InternalMetaStorage implements MetaStorage {

    private IdentifiableStore<ManagedExecution<?>> executions;
    private IdentifiableStore<FormConfig> formConfigs;
    private IdentifiableStore<User> authUser;
    private IdentifiableStore<Role> authRole;
    private IdentifiableStore<Group> authGroup;


    @Getter
    private DatasetRegistry datasetRegistry;
    @Getter
    protected final CentralRegistry centralRegistry = new CentralRegistry();
    @Getter
    protected final Validator validator;

    public InternalMetaStorage(Validator validator, StorageFactory storageFactory, List<String> pathName, DatasetRegistry datasetRegistry) {
        this.datasetRegistry = datasetRegistry;
        this.validator = validator;

        executions = storageFactory.createExecutionsStore(centralRegistry, datasetRegistry, pathName);
        formConfigs = storageFactory.createFormConfigStore(centralRegistry, pathName);
        authUser = storageFactory.createUserStore(centralRegistry, pathName);
        authRole = storageFactory.createRoleStore(centralRegistry, pathName);
        authGroup = storageFactory.createGroupStore(centralRegistry, pathName);
    }

    @Override
    public void loadData() {
        executions.loadData();
        formConfigs.loadData();
        authUser.loadData();
        authRole.loadData();
        authGroup.loadData();
    }

    @Override
    public void clear() {
        executions.clear();
        formConfigs.clear();
        authUser.clear();
        authRole.clear();
        authGroup.clear();
    }

    @Override
    public void remove() {
        executions.removeStore();
        formConfigs.removeStore();
        authUser.removeStore();
        authRole.removeStore();
        authGroup.removeStore();
    }

    @Override
    public void addExecution(ManagedExecution<?> query) {
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
    public void updateExecution(ManagedExecution<?> query) {
        executions.update(query);
    }

    @Override
    public void removeExecution(ManagedExecutionId id) {
        executions.remove(id);
    }

    @Override
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
    public void updateUser(User user) {
        authUser.update(user);
    }

    @Override
    public void updateRole(Role role) {
        authRole.update(role);
    }

    @Override
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
    public void updateGroup(Group group) {
        authGroup.update(group);
    }

    @Override
    public FormConfig getFormConfig(FormConfigId id) {
        return formConfigs.get(id);
    }

    @Override
    public Collection<FormConfig> getAllFormConfigs() {
        return formConfigs.getAll();
    }

    @Override
    public void removeFormConfig(FormConfigId id) {
        formConfigs.remove(id);
    }

    @Override
    @SneakyThrows
    public void updateFormConfig(FormConfig formConfig) {
        formConfigs.update(formConfig);
    }

    @Override
    @SneakyThrows
    public void addFormConfig(FormConfig formConfig) {
        formConfigs.add(formConfig);
    }

    @Override
    public void close() throws IOException {
        executions.close();
        formConfigs.close();
        authUser.close();
        authRole.close();
        authGroup.close();
    }
}
