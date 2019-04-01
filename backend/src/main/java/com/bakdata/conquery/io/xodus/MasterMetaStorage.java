package com.bakdata.conquery.io.xodus;

import java.util.Collection;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;

public interface MasterMetaStorage extends ConqueryStorage {

	void addQuery(ManagedQuery query) throws JSONException;
	ManagedQuery getQuery(ManagedQueryId id);
	Collection<ManagedQuery> getAllQueries();
	void updateQuery(ManagedQuery query) throws JSONException;
	void removeQuery(ManagedQueryId id);
	

	/**
	 * Adds a permission to the storage.
	 * @param permission The permission to add.
	 * @throws JSONException Is throw on a JSON related failure.
	 */
	void addPermission(ConqueryPermission permission) throws JSONException;
	
	/**
	 * Gets all permissions saved in the storage.
	 * @return A collection of the stored permissions
	 */
	Collection<ConqueryPermission> getAllPermissions();
	
	/**
	 * Removes a permission from the storage that has the given id.
	 * @param id The id of the permission that will be deleted.
	 */
	void removePermission(PermissionId id);
	
	/**
	 * Gets the permission with the specified id from the storage.
	 * @param id The id of the permission to be retrieved.
	 * @return The permission with the specified id.
	 */
	ConqueryPermission getPermission(PermissionId id);
	
	
	/**
	 * Adds a user to the storage.
	 * @param user The user to add.
	 * @throws JSONException Is throw on a JSON related failure.
	 */
	void addUser(User user) throws JSONException;
	
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
	 * @throws JSONException Is throw on a JSON related failure.
	 */
	void updateUser(User user) throws JSONException;
	
	/**
	 * Removes a user from the storage that has the given id.
	 * @param id The id of the user that will be deleted.
	 */
	void removeUser(UserId id);
	
	
	/**
	 * Adds a mandator to the storage.
	 * @param mandator The mandator to add.
	 * @throws JSONException Is throw on a JSON related failure.
	 */
	void addMandator(Mandator mandator) throws JSONException;
	
	/**
	 * Gets the mandator with the specified id from the storage.
	 * @param id The id of the mandator to be retrieved.
	 * @return The mandator with the specified id.
	 */
	Mandator getMandator(MandatorId id);
	
	/**
	 * Gets all mandators saved in the storage.
	 * @return A collection of the stored mandators
	 */
	Collection<Mandator> getAllMandators();
	
	/**
	 * Removes a mandator from the storage that has the given id.
	 * @param id The id of the mandator that will be deleted.
	 */
	void removeMandator(MandatorId id);
	
	/**
	 * Updates a stored mandator that is identified by its id.
	 * @param mandator The mandator, which holds the values, to be updated.
	 * @throws JSONException Is throw on a JSON related failure.
	 */
	void updateMandator(Mandator mandator) throws JSONException;
	
	/**
	 * Return the namespaces used in the instance of conquery.
	 * @return The namespaces.
	 */
	Namespaces getNamespaces();
}