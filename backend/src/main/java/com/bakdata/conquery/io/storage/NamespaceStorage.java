package com.bakdata.conquery.io.storage;

import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorage extends NamespacedStorageImpl {

	protected IdentifiableStore<InternToExternMapper> internToExternMappers;
	protected IdentifiableStore<SearchIndex> searchIndexes;
	protected SingletonStore<EntityIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<PreviewConfig> preview;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

	protected Store<String, Integer> entity2Bucket;

	public NamespaceStorage(StoreFactory storageFactory, String pathName) {
		super(storageFactory, pathName);
	}

	@Override
	public void openStores(ObjectMapper objectMapper) {
		super.openStores(objectMapper);

		internToExternMappers = getStorageFactory().createInternToExternMappingStore(super.getPathName(), objectMapper);
		searchIndexes = getStorageFactory().createSearchIndexStore(super.getPathName(), objectMapper);
		idMapping = getStorageFactory().createIdMappingStore(super.getPathName(), objectMapper);
		structure = getStorageFactory().createStructureStore(super.getPathName(), objectMapper);
		workerToBuckets = getStorageFactory().createWorkerToBucketsStore(super.getPathName(), objectMapper);
		preview = getStorageFactory().createPreviewStore(super.getPathName(), objectMapper);
		entity2Bucket = getStorageFactory().createEntity2BucketStore(super.getPathName(), objectMapper);
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(
				dataset,

				internToExternMappers,
				searchIndexes,

				secondaryIds,
				tables,
				imports,

				// Concepts depend on internToExternMappers
				concepts,

				preview,
				idMapping,
				structure,
				workerToBuckets,
				entity2Bucket
		);
	}


	// IdMapping

	public EntityIdMap getIdMapping() {
		return idMapping.get();
	}

	public void updateIdMapping(EntityIdMap idMapping) {
		this.idMapping.update(idMapping);
	}

	// Bucket to Worker Assignment

	public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
		workerToBuckets.update(map);
	}

	public WorkerToBucketsMap getWorkerBuckets() {
		return workerToBuckets.get();
	}

	public int getNumberOfEntities() {
		return entity2Bucket.count();
	}


	public boolean containsEntity(String entity) {
		return entity2Bucket.get(entity) != null;
	}

	public void registerEntity(String entity, int bucket) {
		entity2Bucket.update(entity, bucket);
	}

	// Structure

	public StructureNode[] getStructure() {
		return Objects.requireNonNullElseGet(structure.get(), () -> new StructureNode[0]);
	}

	public void updateStructure(StructureNode[] structure) {
		this.structure.update(structure);
	}

	// InternToExternMappers

	public InternToExternMapper getInternToExternMapper(InternToExternMapperId id) {
		return getInternToExternMapperFromStorage(id);
	}

	private InternToExternMapper getInternToExternMapperFromStorage(InternToExternMapperId id) {
		return internToExternMappers.get(id);
	}

	public void addInternToExternMapper(InternToExternMapper internToExternMapper) {
		internToExternMappers.add(internToExternMapper);
	}

	public void removeInternToExternMapper(InternToExternMapperId id) {
		internToExternMappers.remove(id);
	}

	public Stream<InternToExternMapper> getInternToExternMappers() {
		return internToExternMappers.getAll();
	}

	// SearchIndices

	public SearchIndex getSearchIndex(SearchIndexId id) {
		return getSearchIndexFromStorage(id);
	}

	private SearchIndex getSearchIndexFromStorage(SearchIndexId id) {
		return searchIndexes.get(id);
	}

	public void removeSearchIndex(SearchIndexId id) {
		searchIndexes.remove(id);
	}

	public void addSearchIndex(SearchIndex searchIndex) {
		searchIndexes.add(searchIndex);
	}

	public Stream<SearchIndex> getSearchIndices() {
		return searchIndexes.getAll();
	}

	// PreviewConfig

	public PreviewConfig getPreviewConfig() {
		return preview.get();
	}

	public void setPreviewConfig(PreviewConfig previewConfig){
		preview.update(previewConfig);
	}

	public void removePreviewConfig() {
		preview.remove();
	}

	// Utilities

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return super.inject(values).add(NamespaceStorage.class, this);
	}
}
