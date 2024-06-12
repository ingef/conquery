package com.bakdata.conquery.io.storage;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorage extends NamespacedStorage {

	protected IdentifiableStore<InternToExternMapper> internToExternMappers;
	protected IdentifiableStore<SearchIndex> searchIndexes;
	protected SingletonStore<EntityIdMap> idMapping;
	protected SingletonStore<StructureNode[]> structure;
	protected SingletonStore<PreviewConfig> preview;
	protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

	protected Store<String, Integer> entity2Bucket;

	public NamespaceStorage(StoreFactory storageFactory, String pathName, Validator validator) {
		super(storageFactory, pathName, validator);
	}


	private void decorateIdMapping(SingletonStore<EntityIdMap> idMapping) {
		idMapping
				.onAdd(mapping -> mapping.setStorage(this));
	}

	private void decorateInternToExternMappingStore(IdentifiableStore<InternToExternMapper> store) {
		// We don't call internToExternMapper::init this is done by the first select that needs the mapping
	}


	@Override
	public void openStores(ObjectMapper objectMapper, MetricRegistry metricRegistry) {
		super.openStores(objectMapper, metricRegistry);

		internToExternMappers = getStorageFactory().createInternToExternMappingStore(super.getPathName(), objectMapper);
		searchIndexes = getStorageFactory().createSearchIndexStore(super.getPathName(), objectMapper);
		idMapping = getStorageFactory().createIdMappingStore(super.getPathName(), objectMapper);
		structure = getStorageFactory().createStructureStore(super.getPathName(), objectMapper);
		workerToBuckets = getStorageFactory().createWorkerToBucketsStore(super.getPathName(), objectMapper);
		preview = getStorageFactory().createPreviewStore(super.getPathName(), objectMapper);
		entity2Bucket = getStorageFactory().createEntity2BucketStore(super.getPathName(), objectMapper);

		decorateInternToExternMappingStore(internToExternMappers);
		decorateIdMapping(idMapping);
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




	public EntityIdMap getIdMapping() {
		return idMapping.get();
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

	public int getNumberOfEntities() {
		return entity2Bucket.count();
	}

	public OptionalInt getEntityBucket(String entity) {
		final Integer bucket = entity2Bucket.get(entity);

		if(bucket == null){
			return OptionalInt.empty();
		}

		return OptionalInt.of(bucket);
	}

	public int assignEntityBucket(String entity, int bucketSize) {
		final int bucket = (int) Math.ceil((1d + getNumberOfEntities()) / (double) bucketSize);

		entity2Bucket.add(entity, bucket);

		return bucket;
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

	public Stream<InternToExternMapper> getInternToExternMappers() {
		return internToExternMappers.getAll();
	}

	public void removeSearchIndex(SearchIndexId id) {
		searchIndexes.remove(id);
	}

	public SearchIndex getSearchIndex(SearchIndexId id) {
		return searchIndexes.get(id);
	}

	public void addSearchIndex(SearchIndex searchIndex) {
		searchIndexes.add(searchIndex);
	}

	public Stream<SearchIndex> getSearchIndices() {
		return searchIndexes.getAll();
	}

	public void setPreviewConfig(PreviewConfig previewConfig){
		preview.update(previewConfig);
	}

	public PreviewConfig getPreviewConfig() {
		return preview.get();
	}

	public void removePreviewConfig() {
		preview.remove();
	}

	@Override
	protected <ID extends Id<?> & NamespacedId, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof InternToExternMapperId castId) {
			return (VALUE) getInternToExternMapper(castId);
		}
		if (id instanceof SearchIndexId castId) {
			return (VALUE) getSearchIndex(castId);
		}

		return super.getFromStorage(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return super.inject(values).add(NamespaceStorage.class, this);
	}
}
