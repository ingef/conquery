package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.CachedStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	protected CachedStore<WorkerId, Set<BucketId>> workerToBuckets;
	
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
	@SneakyThrows(JSONException.class)
	public void setWorkerBuckets(WorkerId workerId, Set<BucketId> bucketIdSet) {
		workerToBuckets.update(workerId,bucketIdSet);
	}

	@Override
	public Set<BucketId> getWorkerBuckets(WorkerId workerId) {
		return workerToBuckets.get(workerId);
	}


	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		super.createStores(environmentToStores);
		structure = StoreInfo.STRUCTURE.singleton(getConfig(), environment, getValidator(), new SingletonNamespaceCollection(centralRegistry));
		idMapping = StoreInfo.ID_MAPPING.singleton(getConfig(), environment, getValidator());
		workerToBuckets = StoreInfo.WORKER_TO_BUCKETS.cached(getConfig(), environment, getValidator());

		environmentToStores.putAll(environment, List.of(
			structure,
			idMapping
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
