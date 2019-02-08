package com.bakdata.conquery.io.xodus;

import java.io.File;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.util.functions.Collector;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.Validator;
import java.util.Objects;
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
	public Dictionary getPrimaryDictionary() {
		return dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset()));
	}

	@Override
	public PersistentIdMap getIdMapping() {
		return idMapping.get();
	}


	@Override
	public void updateIdMapping(PersistentIdMap idMapping) throws JSONException {
		this.idMapping.update(idMapping);
	}

	
	protected void createStores(Collector<KeyIncludingStore<?, ?>> collector) {
		super.createStores(collector);
		structure = StoreInfo.STRUCTURE.singleton(this);
		idMapping = StoreInfo.ID_MAPPING.singleton(this);
		collector.collect(structure);
		collector.collect(idMapping);
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
