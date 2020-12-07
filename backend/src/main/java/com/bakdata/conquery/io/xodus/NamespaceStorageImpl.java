package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter @Setter @NonNull
	private MetaStorage metaStorage;
	protected SingletonStore<PersistentIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	
	public NamespaceStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory, true);
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
	@SneakyThrows(JSONException.class)
	public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
		workerToBuckets.update(map);
	}

	public WorkerToBucketsMap getWorkerBuckets() {
		return workerToBuckets.get();
	}

	@Override
	public Set<SecondaryIdDescription> getSecondaryIds() {
		return null;
	}

	@Override
	public void setSecondaryIds(Set<SecondaryIdDescription> secondaryIds) {

	}


	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		super.createStores(environmentToStores);
		structure = StoreInfo.STRUCTURE.singleton(getConfig(), environment, getValidator(), new SingletonNamespaceCollection(centralRegistry));
		idMapping = StoreInfo.ID_MAPPING.singleton(getConfig(), environment, getValidator());
		workerToBuckets = StoreInfo.WORKER_TO_BUCKETS.singleton(getConfig(), environment, getValidator());

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
