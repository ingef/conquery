package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@JsonIgnoreType
public class DatasetRegistry<N extends Namespace> extends IdResolveContext implements Closeable {

	private final ConcurrentMap<DatasetId, N> datasets = new ConcurrentHashMap<>();
	@Getter
	private final int entityBucketSize;

	@Getter
	private final ConqueryConfig config;

	private final InternalObjectMapperCreator internalObjectMapperCreator;

	@Getter
	@Setter
	private MetaStorage metaStorage;
	private final NamespaceHandler<N> namespaceHandler;


	public N createNamespace(Dataset dataset, Validator validator) throws IOException {
		// Prepare empty storage
		NamespaceStorage datasetStorage = new NamespaceStorage(config.getStorage(), "dataset_" + dataset.getName(), validator);
		final ObjectMapper persistenceMapper = internalObjectMapperCreator.createInternalObjectMapper(View.Persistence.Manager.class);

		datasetStorage.openStores(persistenceMapper);
		datasetStorage.loadData();
		datasetStorage.updateDataset(dataset);
		datasetStorage.updateIdMapping(new EntityIdMap());
		datasetStorage.setPreviewConfig(new PreviewConfig());
		datasetStorage.close();

		return createNamespace(datasetStorage);
	}

	public N createNamespace(NamespaceStorage datasetStorage) {
		final N namespace = namespaceHandler.createNamespace(datasetStorage, metaStorage);
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
			metaStorage.getCentralRegistry().remove(removed.getDataset());
			namespaceHandler.removeNamespace(id, removed);
			removed.remove();
		}
	}

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException {
		if (!datasets.containsKey(dataset)) {
			throw new NoSuchElementException(String.format("Did not find Dataset[%s] in [%s]", dataset, datasets.keySet()));
		}

		return datasets.get(dataset).getStorage().getCentralRegistry();
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		return metaStorage.getCentralRegistry();
	}


	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}

	public Collection<N> getDatasets() {
		return datasets.values();
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
		// Make this class also available under DatasetRegistry
		return super.inject(values).add(DatasetRegistry.class, this);
	}

}
