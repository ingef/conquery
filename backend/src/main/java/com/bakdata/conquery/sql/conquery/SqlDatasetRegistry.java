package com.bakdata.conquery.sql.conquery;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SqlDatasetRegistry extends IdResolveContext implements DatasetRegistry {

	private final ConcurrentMap<DatasetId, SqlNamespace> datasets = new ConcurrentHashMap<>();

	@Getter
	private final ConqueryConfig config;

	private final Function<Class<? extends View>, ObjectMapper> internalObjectMapperCreator;

	@Getter
	@Setter
	private MetaStorage metaStorage;

	@Override
	public SqlNamespace createNamespace(Dataset dataset, Validator validator) throws IOException {
		// Prepare empty storage
		NamespaceStorage datasetStorage = new NamespaceStorage(config.getStorage(), "dataset_" + dataset.getName(), validator);
		final ObjectMapper persistenceMapper = internalObjectMapperCreator.apply(View.Persistence.Manager.class);

		datasetStorage.openStores(persistenceMapper);
		datasetStorage.loadData();
		datasetStorage.updateDataset(dataset);
		datasetStorage.updateIdMapping(new EntityIdMap());
		datasetStorage.setPreviewConfig(new PreviewConfig());
		datasetStorage.close();

		return createNamespace(datasetStorage);
	}

	@Override
	public SqlNamespace createNamespace(NamespaceStorage datasetStorage) {
		final SqlNamespace namespace = SqlNamespace.create(new SqlExecutionManager(),
			datasetStorage,
			config,
			internalObjectMapperCreator);

		add(namespace);
		return namespace;
	}

	@Override
	public void add(Namespace ns) {
		// TODO: We could also make DatasetRegistry generic over the namespace
		if (ns instanceof SqlNamespace) {
			datasets.put(ns.getStorage().getDataset().getId(), (SqlNamespace) ns);
		}
		else {
			throw new IllegalArgumentException("Distributed data registry requires distributed namespaces, but got " + ns.getClass());
		}
	}

	@Override
	public SqlNamespace get(DatasetId dataset) {
		return datasets.get(dataset);
	}

	@Override
	public void removeNamespace(DatasetId id) {
		Namespace removed = datasets.remove(id);

		if (removed != null) {
			metaStorage.getCentralRegistry().remove(removed.getDataset());
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

	@Override
	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}

	@Override
	public Collection<SqlNamespace> getDatasets() {
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
