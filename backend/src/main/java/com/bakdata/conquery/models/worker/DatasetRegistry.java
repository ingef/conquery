package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.IndexKey;
import com.bakdata.conquery.models.index.IndexService;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheStats;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@JsonIgnoreType
public class DatasetRegistry<N extends Namespace> implements Closeable, NamespacedStorageProvider, Injectable {

	private final ConcurrentMap<DatasetId, N> datasets = new ConcurrentHashMap<>();
	@Getter
	private final int entityBucketSize;

	@Getter
	private final ConqueryConfig config;

	private final InternalMapperFactory internalMapperFactory;

	private final NamespaceHandler<N> namespaceHandler;

	@Getter
	private final IndexService indexService;

	public N createNamespace(Dataset dataset, MetaStorage metaStorage, Environment environment) throws IOException {
		// Prepare empty storage
		NamespaceStorage datasetStorage = new NamespaceStorage(config.getStorage(), "dataset_" + dataset.getName());
		final ObjectMapper persistenceMapper = internalMapperFactory.createNamespacePersistenceMapper(datasetStorage);

		// Each store injects its own IdResolveCtx so each needs its own mapper
		datasetStorage.openStores(Jackson.copyMapperAndInjectables((persistenceMapper)));
		datasetStorage.updateDataset(dataset);
		datasetStorage.updateIdMapping(new EntityIdMap(datasetStorage));
		datasetStorage.setPreviewConfig(new PreviewConfig());
		datasetStorage.close();

		return createNamespace(datasetStorage, metaStorage, environment);
	}

	public N createNamespace(NamespaceStorage datasetStorage, MetaStorage metaStorage, Environment environment) {
		final N namespace = namespaceHandler.createNamespace(datasetStorage, metaStorage, this, environment);
		add(namespace);
		return namespace;
	}

	public void add(N ns) {
		datasets.put(ns.getStorage().getDataset().getId(), ns);
	}

	public N get(DatasetId dataset) {
		return datasets.get(dataset);
	}

	public void removeNamespace(DatasetId id) {
		N removed = datasets.remove(id);

		if (removed != null) {
			namespaceHandler.removeNamespace(id, removed);
			removed.remove();
		}
	}

	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}

	public Collection<N> getNamespaces() {
		return datasets.values();
	}

	public Set<IndexKey<?>> getLoadedIndexes() {
		return indexService.getLoadedIndexes();
	}

	public CacheStats getIndexServiceStatistics() {
		return indexService.getStatistics();
	}

	@Override
	public void close() {
		for (Namespace namespace : datasets.values()) {
			try {
				namespace.close();
			}
			catch (Exception e) {
				log.error("Unable to close namespace {}", namespace, e);
			}
		}
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		indexService.inject(values);
		// Make this class also available under DatasetRegistry
		return values.add(NamespacedStorageProvider.class, this)
					 .add(this.getClass(), this);
	}

	public void resetIndexService() {
		indexService.evictCache();
	}

	@Override
	public NamespacedStorage getStorage(DatasetId datasetId) {
		return datasets.get(datasetId).getStorage();
	}

	@Override
	public Collection<DatasetId> getAllDatasetIds() {
		return datasets.keySet();
	}
}
