package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

public class NamespaceStorageXodus extends NamespacedStorageXodus implements NamespaceStorage {

	@Getter @Setter @NonNull
	private MetaStorage metaStorage;
	protected SingletonStore<PersistentIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;



	public static NamespaceStorageXodus tryLoad(Validator validator, XodusStorageFactory config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getName(), t));
		env.close();

		if(!exists) {
			return null;
		}

		NamespaceStorageXodus storage = new NamespaceStorageXodus(validator, directory, config);
		storage.loadData();
		return storage;
	}

	public NamespaceStorageXodus(Validator validator, File directory, XodusStorageFactory config) {
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
	public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
		workerToBuckets.update(map);
	}

	public WorkerToBucketsMap getWorkerBuckets() {
		return workerToBuckets.get();
	}

	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		super.createStores(environmentToStores);
		structure = StoreInfo.STRUCTURE.singleton(getConfig().createStore(environment, getValidator(), StoreInfo.STRUCTURE), new SingletonNamespaceCollection(centralRegistry));
		idMapping = StoreInfo.ID_MAPPING.singleton(getConfig().createStore(environment, getValidator(), StoreInfo.ID_MAPPING));
		workerToBuckets = StoreInfo.WORKER_TO_BUCKETS.singleton(getConfig().createStore(environment, getValidator(), StoreInfo.WORKER_TO_BUCKETS));

		environmentToStores.putAll(environment, List.of(
			structure,
			idMapping,
			workerToBuckets
			));
	}

	@Override
	public StructureNode[] getStructure() {
		return Objects.requireNonNullElseGet(structure.get(), ()->new StructureNode[0]);
	}

	@Override
	public void updateStructure(StructureNode[] structure) throws JSONException {
		this.structure.update(structure);
	}
}
