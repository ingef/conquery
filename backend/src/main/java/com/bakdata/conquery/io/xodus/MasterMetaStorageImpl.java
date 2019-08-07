package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.functions.Collector;

import lombok.Getter;

public class MasterMetaStorageImpl extends ConqueryStorageImpl implements MasterMetaStorage, ConqueryStorage {
	
	private SingletonStore<Namespaces> meta;
	private IdentifiableStore<ManagedExecution> executions;
	private IdentifiableStore<User> authUser;
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
		this.executions = StoreInfo.EXECUTIONS.<ManagedExecution>identifiable(this, namespaces)
			.onAdd(value-> value.initExecutable(namespaces.get(value.getDataset())));
		
		MasterMetaStorage storage = this;
		this.authMandator = StoreInfo.AUTH_MANDATOR.<Mandator>identifiable(storage);
		this.authUser = StoreInfo.AUTH_USER.<User>identifiable(storage);
		
		collector
			.collect(meta)
			.collect(authMandator)
			//load users before queries
			.collect(authUser)
			.collect(executions);
	}

	@Override
	public void addExecution(ManagedExecution query) throws JSONException {
		executions.add(query);
	}

	@Override
	public ManagedExecution getExecution(ManagedExecutionId id) {
		return executions.get(id);
	}

	@Override
	public Collection<ManagedExecution> getAllExecutions() {
		return executions.getAll();
	}

	@Override
	public void updateExecution(ManagedExecution query) throws JSONException {
		executions.update(query);
	}

	@Override
	public void removeExecution(ManagedExecutionId id) {
		executions.remove(id);
	}
	
	@Override
	public void addUser(User user) throws JSONException {
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
	public void addMandator(Mandator mandator) throws JSONException {
		authMandator.add(mandator);
	}
	
	@Override
	public Mandator getMandator(MandatorId mandatorId) {
		return authMandator.get(mandatorId);
	}
	
	@Override
	public Collection<Mandator> getAllMandators() {
		return authMandator.getAll();
	}
	
	@Override
	public void removeMandator(MandatorId mandatorId)  {
		authMandator.remove(mandatorId);
	}

	@Override
	public void updateUser(User user) throws JSONException {
		authUser.update(user);
	}

	@Override
	public void updateMandator(Mandator mandator) throws JSONException {
		authMandator.update(mandator);
	}
}
