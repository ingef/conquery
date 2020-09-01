package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.util.functions.Collector;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter @Setter @NonNull
	private MetaStorage metaStorage;
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
	protected void createStores(Collector<Environment, KeyIncludingStore<?, ?>> collector) {
		super.createStores(collector);
		structure = StoreInfo.STRUCTURE.singleton(getConfig(), environment, getValidator(), new SingletonNamespaceCollection(centralRegistry));
		idMapping = StoreInfo.ID_MAPPING.singleton(getConfig(), environment, getValidator());
		collector
			.collect(environment, structure)
			.collect(environment, idMapping);
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
