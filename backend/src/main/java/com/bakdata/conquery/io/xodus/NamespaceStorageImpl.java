package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter @Setter @NonNull
	private MasterMetaStorage metaStorage;
	protected SingletonStore<PersistentIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	
	public NamespaceStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
	}


	@Override
	public PersistentIdMap getIdMapping() {
		return idMapping.get();
	}


	@Override
	public void updateIdMapping(PersistentIdMap idMapping) throws JSONException {
		this.idMapping.update(idMapping);
	}

	@Override
	protected List<ListenableFuture<KeyIncludingStore<?, ?>>> createStores(ListeningExecutorService pool) throws ExecutionException, InterruptedException {

		// Await super first, then load structure and idmapping
		final List<ListenableFuture<KeyIncludingStore<?, ?>>> stores = super.createStores(pool);
		Futures.allAsList(stores).get();

		structure = StoreInfo.STRUCTURE.singleton(getEnvironment(), getValidator(), new SingletonNamespaceCollection(centralRegistry));

		idMapping = StoreInfo.ID_MAPPING.singleton(getEnvironment(), getValidator());


		return ImmutableList.<ListenableFuture<KeyIncludingStore<?, ?>>>builder()
					   .addAll(stores)
					   .add(
							   pool.submit(() -> {
								   structure.loadData();
								   return structure;
							   }),
							   pool.submit(() -> {
								   idMapping.loadData();
								   return idMapping;
							   })
					   )
					   .build();

	}

	@Override
	public StructureNode[] getStructure() {
		return Objects.requireNonNullElseGet(structure.get(), () -> new StructureNode[0]);
	}

	@Override
	public void updateStructure(StructureNode[] structure) throws JSONException {
		this.structure.update(structure);
	}
}
