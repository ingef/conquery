package com.bakdata.conquery.io.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
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
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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

		if (dictionary == null) {
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

		final MutableGraph<KeyIncludingStore<?, ?>> loadGraph = getStoreDependencies();

		//TODO this can also be used to load data in parallel
		for (KeyIncludingStore<?, ?> store : Traverser.forGraph(loadGraph).breadthFirst(dataset)) {
			store.loadData();
		}

		log.info("Done reading {}", getDataset());
	}

	@NotNull
	private MutableGraph<KeyIncludingStore<?, ?>> getStoreDependencies() {
		MutableGraph<KeyIncludingStore<?, ?>> loadGraph =
				GraphBuilder.directed()
							.allowsSelfLoops(false)
							.build();

		loadGraph.addNode(dataset);

		loadGraph.putEdge(dataset, secondaryIds);
		loadGraph.putEdge(dataset, dictionaries);
		loadGraph.putEdge(dataset, internToExternMappers);
		loadGraph.putEdge(dataset, primaryDictionary);

		loadGraph.putEdge(secondaryIds, tables);

		loadGraph.putEdge(tables, imports);
		loadGraph.putEdge(tables, concepts);

		loadGraph.putEdge(internToExternMappers, concepts);

		loadGraph.putEdge(concepts, structure);

		loadGraph.putEdge(dictionaries,idMapping);

		loadGraph.putEdge(imports, workerToBuckets);
		return loadGraph;
	}

	@Override
	public void clear() {
		centralRegistry.clear();

		dataset.clear();
		secondaryIds.clear();
		tables.clear();
		dictionaries.clear();
		imports.clear();
		concepts.clear();

		internToExternMappers.clear();
		idMapping.clear();
		structure.clear();
		workerToBuckets.clear();
		primaryDictionary.clear();

	}

	@Override
	public void removeStorage() {
		dataset.removeStore();
		secondaryIds.removeStore();
		tables.removeStore();
		dictionaries.removeStore();
		imports.removeStore();
		concepts.removeStore();

		internToExternMappers.removeStore();
		idMapping.removeStore();
		structure.removeStore();
		workerToBuckets.removeStore();
		primaryDictionary.removeStore();

	}

	@Override
	public void close() throws IOException {
		dataset.close();
		secondaryIds.close();
		tables.close();
		dictionaries.close();
		imports.close();
		concepts.close();

		internToExternMappers.close();
		idMapping.close();
		structure.close();
		workerToBuckets.close();
		primaryDictionary.close();

	}

	public EntityIdMap getIdMapping() {
		return idMapping.get();
	}


	public void updatePrimaryDictionary(Dictionary dictionary) {
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
