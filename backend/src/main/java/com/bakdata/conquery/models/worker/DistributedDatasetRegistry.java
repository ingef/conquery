package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.messages.namespaces.specific.ShutdownShard;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds the necessary information about all datasets on the {@link ManagerNode}. This includes meta data of each dataset (not to confuse
 * with {@link MetaStorage}) as well as informations about the distributed query engine.
 */
@Slf4j
@RequiredArgsConstructor
@JsonIgnoreType
public class DistributedDatasetRegistry extends IdResolveContext implements DatasetRegistry {

	private final ConcurrentMap<DatasetId, DistributedNamespace> datasets = new ConcurrentHashMap<>();
	@Getter
	private final int entityBucketSize;

	@Getter
	private final ConcurrentMap<SocketAddress, ShardNodeInformation> shardNodes = new ConcurrentHashMap<>();

	@Getter
	private final ConqueryConfig config;

	private final Function<Class<? extends View>, ObjectMapper> internalObjectMapperCreator;

	@Getter
	@Setter
	private MetaStorage metaStorage;

	@Override
	public Namespace createNamespace(Dataset dataset, Validator validator) throws IOException {
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
	public Namespace createNamespace(NamespaceStorage datasetStorage) {
		final DistributedNamespace namespace = DistributedNamespace.create(new DistributedExecutionManager(getMetaStorage()),
			datasetStorage,
			config,
			internalObjectMapperCreator);

		add(namespace);

		// for now we just add one worker to every ShardNode
		for (ShardNodeInformation node : getShardNodes().values()) {
			node.send(new AddWorker(datasetStorage.getDataset()));
		}

		return namespace;
	}

	@Override
	public void add(Namespace ns) {
		// TODO: We could also make DatasetRegistry generic over the namespace
		if (ns instanceof DistributedNamespace) {
			datasets.put(ns.getStorage().getDataset().getId(), (DistributedNamespace) ns);
		} else {
			throw new IllegalArgumentException("Distributed data registry requires distributed namespaces, but got " + ns.getClass());
		}
	}

	@Override
	public DistributedNamespace get(DatasetId dataset) {
		return datasets.get(dataset);
	}

	@Override
	public void removeNamespace(DatasetId id) {
		DistributedNamespace removed = datasets.remove(id);

		if (removed != null) {
			metaStorage.getCentralRegistry().remove(removed.getDataset());

			getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(removed.getDataset())));
			removed.getWorkerHandler().removeDataset(id);
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

	public synchronized void register(ShardNodeInformation node, WorkerInformation info) {
		DistributedNamespace namespace = datasets.get(info.getDataset());
		if (namespace == null) {
			throw new NoSuchElementException("Trying to register a worker for unknown dataset '" + info.getDataset() + "'. I only know " + datasets.keySet());
		}
		namespace.getWorkerHandler().register(node, info);
	}

	public WorkerInformation getWorker(final WorkerId workerId, final DatasetId id) {
		return Optional
			.ofNullable(datasets.get(id))
			.flatMap(ns -> ns.getWorkerHandler().getWorkers().getOptional(workerId))
			.orElseThrow(() -> new NoSuchElementException("Unknown worker worker '%s' for dataset '%s'".formatted(workerId, id)));
	}

	@Override
	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}

	@Override
	public Collection<DistributedNamespace> getDatasets() {
		return datasets.values();
	}

	@Override
	public void close() {
		getShardNodes().forEach(((socketAddress, shardNodeInformation) -> shardNodeInformation.send(new ShutdownShard())));
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
