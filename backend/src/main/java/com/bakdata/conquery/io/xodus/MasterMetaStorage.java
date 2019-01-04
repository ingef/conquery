package com.bakdata.conquery.io.xodus;

import java.util.Collection;
import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;

public interface MasterMetaStorage extends ConqueryStorage {

	void addQuery(ManagedQuery query) throws JSONException;
	ManagedQuery getQuery(ManagedQueryId id);
	Collection<ManagedQuery> getAllQueries();
	void updateQuery(ManagedQuery query) throws JSONException;
	void removeQuery(ManagedQueryId id);
	

	void addPermission(ConqueryPermission permission) throws JSONException;
	Collection<ConqueryPermission> getAllPermissions();
	void removePermission(PermissionId permissionId);
	ConqueryPermission getPermission(PermissionId id);
	Set<ConqueryPermission> getPermissions(PermissionOwnerId<?> owner);
	
	void addUser(User user) throws JSONException;
	User getUser(UserId user);
	Collection<User> getAllUsers();
	void updateUser(User user) throws JSONException;
	void removeUser(UserId userId);
	
	void addMandator(Mandator mandator) throws JSONException;
	Mandator getMandator(MandatorId mandatorId);
	Collection<Mandator> getAllMandators();
	void removeMandator(MandatorId mandatorId);
}
