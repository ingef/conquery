package com.bakdata.conquery.io.xodus;

import java.util.Collection;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.Namespaces;

/**
 * Interface for the persistent data of the {@link ManagerNode} 
 */
public interface MetaStorage extends ConqueryStorage {

	void addExecution(ManagedExecution<?> query);
	ManagedExecution getExecution(ManagedExecutionId id);
	Collection<ManagedExecution<?>> getAllExecutions();
	void updateExecution(ManagedExecution<?> query) throws JSONException;
	void removeExecution(ManagedExecutionId id);
	
	/**
	 * Adds a user to the storage.
	 * @param user The user to add.
	 */
	void addUser(User user) ;
	
	/**
	 * Gets the user with the specified id from the storage.
	 * @param id The id of the user to be retrieved.
	 * @return The user with the specified id.
	 */
	User getUser(UserId id);
	
	/**
	 * Gets all users saved in the storage.
	 * @return A collection of the stored users
	 */
	Collection<User> getAllUsers();
	
	/**
	 * Updates a stored user that is identified by its id.
	 * @param user The user, which holds the values, to be updated.
	 */
	void updateUser(User user);
	
	/**
	 * Removes a user from the storage that has the given id.
	 * @param id The id of the user that will be deleted.
	 */
	void removeUser(UserId id);
	
	
	/**
	 * Adds a role to the storage.
	 * @param role The role to add.
	 */
	void addRole(Role role) ;
	
	/**
	 * Gets the role with the specified id from the storage.
	 * @param id The id of the role to be retrieved.
	 * @return The role with the specified id.
	 */
	Role getRole(RoleId id);
	
	/**
	 * Gets all roles saved in the storage.
	 * @return A collection of the stored roles
	 */
	Collection<Role> getAllRoles();
	
	/**
	 * Removes a role from the storage that has the given id.
	 * @param id The id of the role that will be deleted.
	 */
	void removeRole(RoleId id);
	
	/**
	 * Updates a stored role that is identified by its id.
	 * @param role The role, which holds the values, to be updated.
	 */
	void updateRole(Role role) ;
	
	/**
	 * Adds a role to the storage.
	 * @param role The role to add.
	 */
	void addGroup(Group group) ;
	
	/**
	 * Gets the Group with the specified id from the storage.
	 * @param id The id of the Group to be retrieved.
	 * @return The Group with the specified id.
	 */
	Group getGroup(GroupId id);
	
	/**
	 * Gets all Groups saved in the storage.
	 * @return A collection of the stored Groups
	 */
	Collection<Group> getAllGroups();
	
	/**
	 * Removes a Group from the storage that has the given id.
	 * @param id The id of the Group that will be deleted.
	 */
	void removeGroup(GroupId id);
	
	/**
	 * Updates a stored Group that is identified by its id.
	 * @param Group The Group, which holds the values, to be updated.
	 */
	void updateGroup(Group group);
	
	/**
	 * Return the namespaces used in the instance of conquery.
	 * @return The namespaces.
	 */
	Namespaces getNamespaces();
	
	/**
	 * Gets the FormConfig with the specified id from the storage.
	 * @param id The id of the FormConfig to be retrieved.
	 * @return The FormConfig with the specified id.
	 */
	FormConfig getFormConfig(FormConfigId id);
	
	/**
	 * Gets all FormConfigs saved in the storage.
	 * @return A collection of the stored FormConfigs
	 */
	Collection<FormConfig> getAllFormConfigs();
	
	/**
	 * Removes a FormConfig from the storage that has the given id.
	 * @param id The id of the FormConfig that will be deleted.
	 */
	void removeFormConfig(FormConfigId id);
	
	/**
	 * Updates a stored FormConfig that is identified by its id.
	 * @param FormConfig The FormConfig, which holds the values, to be updated.
	 */
	void updateFormConfig(FormConfig config);
	
	/**
	 * Adds a FormConfig that is identified by its id.
	 * @param FormConfig The FormConfig, which holds the values, to be updated.
	 */
	void addFormConfig(FormConfig formConfig);
}