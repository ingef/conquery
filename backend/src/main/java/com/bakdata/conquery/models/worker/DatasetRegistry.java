package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds the necessary information about all datasets on the {@link ManagerNode}.
 * This includes meta data of each dataset (not to confuse with {@link MetaStorage}) as well as informations about the
 * distributed query engine.
 */
@Slf4j
@RequiredArgsConstructor
@JsonIgnoreType
public class DatasetRegistry extends IdResolveContext implements Closeable {

	private final ConcurrentMap<DatasetId, Namespace> datasets = new ConcurrentHashMap<>();
	@NotNull
	@Getter
	@Setter
	private IdMap<WorkerId, WorkerInformation> workers = new IdMap<>(); // TODO remove this and take it from Namespaces.datasets

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


	private final Map<DatasetId, CountDownLatch> addLatches = new ConcurrentHashMap<>();
	private final Map<DatasetId, CountDownLatch> deletionLatches = new ConcurrentHashMap<>();


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


	public Namespace createNamespace(NamespaceStorage datasetStorage) {


		final Namespace namespace = Namespace.create(
				new ExecutionManager(getMetaStorage()),
				datasetStorage,
				config,
				internalObjectMapperCreator
		);

		final DatasetId datasetId = namespace.getStorage().getDataset().getId();

		datasets.put(datasetId, namespace);

		CountDownLatch latch = addLatches.get(datasetId);
		synchronized (this) {
			if (latch != null && latch.getCount() > 0) {
				throw new IllegalStateException(String.format("There exists a adding latch. A adding of a namespace '%s' is in progress. Skipping", datasetId));
			}

			// Size the countdown to the number of shards/workers
			latch = new CountDownLatch(getShardNodes().size());
			addLatches.put(datasetId, latch);

			// for now we just add one worker to every ShardNode
			for (ShardNodeInformation node : getShardNodes().values()) {
				node.send(new AddWorker(datasetStorage.getDataset()));
			}
		}

		try {

			// Wait till all adding reports are in
			if (latch.await(2, TimeUnit.MINUTES)) {
				log.info("Creation of namespace {} successful", namespace);
				return namespace;
			}
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(String.format("Was interrupted while waiting for creation of workers for dataset: '%s'", datasetId));
		}

		log.warn("Ran into timeout. Adding takes longer than expected.");
		return namespace;
	}

	public void acknowledgeWorkerCreation(WorkerId workerId) {
		Preconditions.checkNotNull(addLatches.get(workerId.getDataset())).countDown();
	}

	public Namespace get(DatasetId dataset) {
		return datasets.get(dataset);
	}

	public void acknowledgeWorkerDeletion(WorkerId workerId) {
		Preconditions.checkNotNull(deletionLatches.get(workerId.getDataset())).countDown();
	}

	public void removeNamespace(DatasetId datasetId) throws InterruptedException {
		Namespace removed = datasets.remove(datasetId);


		if (removed == null) {
			log.info("Dataset '{}' was already removed", datasetId);
			return;
		}

		CountDownLatch latch = deletionLatches.get(datasetId);
		synchronized (this) {
			if (latch != null && latch.getCount() > 0) {
				log.error("There exists a deletion latch. A deletion is in progress for dataset '{}'. Skipping", datasetId);
				return;
			}

			// Size the countdown to the number of workers
			latch = new CountDownLatch(removed.getWorkers().size());
			deletionLatches.put(datasetId, latch);
		}


		metaStorage.getCentralRegistry().remove(removed.getDataset());

		// Trigger deletion of workers
		getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(removed.getDataset())));

		workers.keySet().removeIf(w -> w.getDataset().equals(datasetId));
		removed.remove();

		// Wait till all remove reports are in
		if (latch.await(2, TimeUnit.MINUTES)) {
			log.info("Deletion of namespace {} successful", removed);
			return;
		}

		log.warn("Ran into timeout. Deletion takes longer than expected.");

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
		WorkerInformation old = workers.getOptional(info.getId()).orElse(null);
		if (old != null) {
			old.setIncludedBuckets(info.getIncludedBuckets());
			old.setConnectedShardNode(node);
		}
		else {
			info.setConnectedShardNode(node);
			workers.add(info);
		}

		Namespace ns = datasets.get(info.getDataset());
		if (ns == null) {
			throw new NoSuchElementException(
					"Trying to register a worker for unknown dataset '" + info.getDataset() + "'. I only know " + datasets.keySet());
		}
		ns.addWorker(info);

		// There might be no latch because the dataset is not new but loaded from a store
		addLatches.computeIfAbsent(info.getDataset(), (_k) -> new CountDownLatch(0)).countDown();
	}

	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}

	public Collection<Namespace> getDatasets() {
		return datasets.values();
	}

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
		// Make this class also availiable under DatasetRegistry
		return super.inject(values)
					.add(DatasetRegistry.class, this);
	}
}
