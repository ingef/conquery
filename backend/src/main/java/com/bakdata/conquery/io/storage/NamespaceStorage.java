package com.bakdata.conquery.io.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorage extends NamespacedStorage {

	protected IdentifiableStore<InternToExternMapper> internToExternMappers;
	protected SingletonStore<EntityIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

	protected SingletonStore<Dictionary> primaryDictionary;

	public NamespaceStorage(StoreFactory storageFactory, Validator validator, String pathName) {
		super(storageFactory, validator, pathName);
	}

	public EncodedDictionary getPrimaryDictionary() {
		return new EncodedDictionary(getPrimaryDictionaryRaw(), StringTypeEncoded.Encoding.UTF8);
	}

	@NonNull
	public Dictionary getPrimaryDictionaryRaw() {
		final Dictionary dictionary = primaryDictionary.get();

		if(dictionary == null){
			log.trace("No prior PrimaryDictionary, creating one");
			final MapDictionary newPrimary = new MapDictionary(getDataset(), ConqueryConstants.PRIMARY_DICTIONARY);

			primaryDictionary.update(newPrimary);

			return newPrimary;
		}

		return dictionary;
	}


	private void decorateIdMapping(SingletonStore<EntityIdMap> idMapping) {
		idMapping
				.onAdd(mapping -> mapping.setStorage(this));
	}

	private void decorateInternToExternMappingStore(IdentifiableStore<InternToExternMapper> store) {
		// We don't call internToExternMapper::init this is done by the first select that needs the mapping
	}

	@Override
	public void openStores(ObjectMapper objectMapper) {
		super.openStores(objectMapper);

		internToExternMappers = getStorageFactory().createInternToExternMappingStore(super.getPathName(), getCentralRegistry(), objectMapper);
		idMapping = getStorageFactory().createIdMappingStore(super.getPathName(), objectMapper);
		structure = getStorageFactory().createStructureStore(super.getPathName(), getCentralRegistry(), objectMapper);
		workerToBuckets = getStorageFactory().createWorkerToBucketsStore(super.getPathName(), objectMapper);
		primaryDictionary = getStorageFactory().createPrimaryDictionaryStore(super.getPathName(), getCentralRegistry(), objectMapper);

		decorateInternToExternMappingStore(internToExternMappers);
		decorateIdMapping(idMapping);
	}

	@Override
	public void loadData() {
		super.loadData();

		internToExternMappers.loadData();
		idMapping.loadData();
		structure.loadData();
		workerToBuckets.loadData();
		primaryDictionary.loadData();
	}

	@Override
	public void clear() {
		super.clear();
		internToExternMappers.clear();
		idMapping.clear();
		structure.clear();
		workerToBuckets.clear();
		primaryDictionary.clear();

	}

	@Override
	public void removeStorage() {
		super.removeStorage();

		internToExternMappers.removeStore();
		idMapping.removeStore();
		structure.removeStore();
		workerToBuckets.removeStore();
		primaryDictionary.removeStore();

	}

	@Override
	public void close() throws IOException {
		super.close();

		internToExternMappers.close();
		idMapping.close();
		structure.close();
		workerToBuckets.close();
		primaryDictionary.close();

	}

	public EntityIdMap getIdMapping() {
		return idMapping.get();
	}


	public void updatePrimaryDictionary(Dictionary dictionary){
		primaryDictionary.update(dictionary);
	}

	public void updateIdMapping(EntityIdMap idMapping) {
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

	public InternToExternMapper getInternToExternMapper(InternToExternMapperId id) {
		return internToExternMappers.get(id);
	}

	public void addInternToExternMapper(InternToExternMapper internToExternMapper) {
		internToExternMappers.add(internToExternMapper);
	}

	public void removeInternToExternMapper(InternToExternMapperId id) {
		internToExternMappers.remove(id);
	}

	public Collection<InternToExternMapper> getInternToExternMappers() {
		return internToExternMappers.getAll();
	}
}
