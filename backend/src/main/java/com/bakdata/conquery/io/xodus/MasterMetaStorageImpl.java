package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.functions.Collector;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterMetaStorageImpl extends ConqueryStorageImpl implements MasterMetaStorage, ConqueryStorage {
	
	private SingletonStore<Namespaces> meta;
	private IdentifiableStore<ManagedQuery> queries;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<ConqueryPermission> authPermissions;
	private IdentifiableStore<Mandator> authMandator;
	
	@Getter
	private Namespaces namespaces;

	public MasterMetaStorageImpl(Namespaces namespaces, Validator validator, StorageConfig config) {
		super(
			validator,
			config,
			new File(config.getDirectory(), "meta")
		);
		this.namespaces = namespaces;
	}

	@Override
	protected void createStores(Collector<KeyIncludingStore<?, ?>> collector) {
		this.meta = StoreInfo.NAMESPACES.singleton(this);
		this.queries = StoreInfo.QUERIES.identifiable(this, namespaces);
		
		MasterMetaStorage storage = this;
		this.authMandator = StoreInfo.AUTH_MANDATOR.<Mandator>identifiable(storage);
		this.authUser = StoreInfo.AUTH_USER.<User>identifiable(storage);
		this.authPermissions = StoreInfo.AUTH_PERMISSIONS.<ConqueryPermission>identifiable(storage)
			.onAdd(value->		value.getOwnerId().getOwner(storage).addPermissionLocal(value));
		
		collector
			.collect(meta)
			.collect(authMandator)
			//load users before queries
			.collect(authUser)
			.collect(queries)
			.collect(authPermissions);
	}

	@Override
	public void addQuery(ManagedQuery query) throws JSONException {
		queries.add(query);
	}

	@Override
	public ManagedQuery getQuery(ManagedQueryId id) {
		return queries.get(id);
	}

	@Override
	public Collection<ManagedQuery> getAllQueries() {
		return queries.getAll();
	}

	@Override
	public void updateQuery(ManagedQuery query) throws JSONException {
		queries.update(query);
	}

	@Override
	public void removeQuery(ManagedQueryId id) {
		queries.remove(id);
	}
	
	/*
	@Override
	public Namespaces getMeta() {
		return meta.get();
	}

	@Override
	public void updateMeta(Namespaces meta) throws JSONException {
		this.meta.update(meta);
		//see #147 ?
		/*
		if(blockManager != null) {
			blockManager.init(slaveInfo);
		}
		*/
	//}
	
	public void addPermission(ConqueryPermission permission) throws JSONException {
		authPermissions.add(permission);
	}
	
	public Collection<ConqueryPermission> getAllPermissions() {
		return authPermissions.getAll();
	}
	
	public void removePermission(PermissionId permissionId) {
		authPermissions.remove(permissionId);
	}
	
	public void addUser(User user) throws JSONException {
		authUser.add(user);
	}
	
	public User getUser(UserId userId) {
		return authUser.get(userId);
	}
	
	public Collection<User> getAllUsers() {
		return authUser.getAll();
	}
	
	public void removeUser(UserId userId) {
		authUser.remove(userId);
	}

	public void addMandator(Mandator mandator) throws JSONException {
		authMandator.add(mandator);
	}
	
	public Mandator getMandator(MandatorId mandatorId) {
		return authMandator.get(mandatorId);
	}
	
	@Override
	public Collection<Mandator> getAllMandators() {
		return authMandator.getAll();
	}
	
	public void removeMandator(MandatorId mandatorId)  {
		authMandator.remove(mandatorId);
	}

	@Override
	public void updateUser(User user) throws JSONException {
		authUser.update(user);
	}

	@Override
	public ConqueryPermission getPermission(PermissionId id) {
		return authPermissions.get(id);
	}

	@Override
	public void updateMandator(Mandator mandator) throws JSONException {
		authMandator.update(mandator);
	}
}
