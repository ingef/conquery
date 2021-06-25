package com.bakdata.conquery.io.storage;

import java.io.IOException;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorage extends NamespacedStorage {

	@Getter
	@Setter
	@NonNull
	private MetaStorage metaStorage;
	protected SingletonStore<PersistentIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

	@Getter
	private final boolean registerImports = true;

	public NamespaceStorage(Validator validator, StoreFactory storageFactory, String pathName) {
		super(validator, storageFactory, pathName);

		idMapping = storageFactory.createIdMappingStore(pathName);
		structure = storageFactory.createStructureStore(pathName, new SingletonNamespaceCollection(getCentralRegistry()));
		workerToBuckets = storageFactory.createWorkerToBucketsStore(pathName);
	}

	@Override
	public void loadData() {
		super.loadData();

		idMapping.loadData();
		structure.loadData();
		workerToBuckets.loadData();
	}

	@Override
	public void clear() {
		super.clear();
		idMapping.clear();
		structure.clear();
		workerToBuckets.clear();
	}

	@Override
	public void removeStorage() {
		super.removeStorage();
		idMapping.removeStore();
		structure.removeStore();
		workerToBuckets.removeStore();
	}

	@Override
	public void close() throws IOException {
		super.close();
		idMapping.close();
		structure.close();
		workerToBuckets.close();
	}

	public PersistentIdMap getIdMapping() {
		return idMapping.get();
	}


	public void updateIdMapping(PersistentIdMap idMapping) {
		this.idMapping.update(idMapping);
	}

	public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
		workerToBuckets.update(map);
	}

	public WorkerToBucketsMap getWorkerBuckets() {
		return workerToBuckets.get();
	}


	public StructureNode[] getStructure() {
		return Objects.requireNonNullElseGet(structure.get(), () -> new StructureNode[0]);
	}

	public void updateStructure(StructureNode[] structure) {
		this.structure.update(structure);
	}
}
