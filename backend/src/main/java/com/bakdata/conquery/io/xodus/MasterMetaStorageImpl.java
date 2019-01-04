package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterMetaStorageImpl extends ConqueryStorageImpl implements MasterMetaStorage, ConqueryStorage {
	
	private final SingletonStore<Namespaces> meta;
	private final IdentifiableStore<ManagedQuery> queries;

	public MasterMetaStorageImpl(Validator validator, StorageConfig config) {
		super(
			validator, 
			config,
			new File(new File(config.getDirectory(), "master"), "meta")
		);
		this.meta = StoreInfo.NAMESPACES.singleton(this);
		this.queries = StoreInfo.QUERIES.identifiable(this);
	}

	@Override
	public void stopStores() throws IOException {
		queries.close();
		meta.close();
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
		//TODO?
		/*
		if(blockManager != null) {
			blockManager.init(slaveInfo);
		}
		*/
	//}
}
