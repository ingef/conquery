package com.bakdata.conquery.models.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the main routine to load data into Conquery.
 */
@RequiredArgsConstructor
@Slf4j
public class ImportJob extends Job {

	private final DistributedNamespace namespace;

	@Getter
	private final Table table;
	private final int bucketSize;
	private final PreprocessedHeader header;
	private final PreprocessedData container;
	private final ConqueryConfig config;

	private final IdMutex<DictionaryId> sharedDictionaryLocks;


	private static final int NUMBER_OF_STEPS = /* directly in execute = */4;

	public static ImportJob createOrUpdate(DistributedNamespace namespace, InputStream inputStream, int entityBucketSize, IdMutex<DictionaryId> sharedDictionaryLocks, ConqueryConfig config, boolean update)
			throws IOException {

		try (PreprocessedReader parser = new PreprocessedReader(inputStream, namespace.getPreprocessMapper())) {

			final Dataset ds = namespace.getDataset();

			// We parse semi-manually as the incoming file consist of multiple documents we only read progressively:
			// 1) the header to check metadata
			// 2) The Dictionaries to be imported and transformed
			// 3) The ColumnStores themselves which contain references to the previously imported dictionaries.


			final PreprocessedHeader header = parser.readHeader();

			final TableId tableId = new TableId(ds.getId(), header.getTable());
			final Table table = namespace.getStorage().getTable(tableId);

			if (table == null) {
				throw new BadRequestException(String.format("Table[%s] does not exist.", tableId));
			}

			// Ensure that Import and Table have the same schema
			header.assertMatch(table);

			final ImportId importId = new ImportId(table.getId(), header.getName());
			final Import processedImport = namespace.getStorage().getImport(importId);

			if (update) {
				if (processedImport == null) {
					throw new WebApplicationException(String.format("Import[%s] is not present.", importId), Response.Status.NOT_FOUND);
				}
				// before updating the import, make sure that all workers removed the last import
				namespace.getWorkerHandler().sendToAll(new RemoveImportJob(processedImport));
				namespace.getStorage().removeImport(importId);
			}
			else if (processedImport != null) {
				throw new WebApplicationException(String.format("Import[%s] is already present.", importId), Response.Status.CONFLICT);
			}


			log.trace("Begin reading data.");

			final PreprocessedData container = parser.readData();

			log.debug("Done reading data. Contains {} Entities.", container.size());

			log.info("Importing {} into {}", header.getName(), tableId);

			return new ImportJob(
					namespace,
					table,
					entityBucketSize,
					header,
					container,
					config
			);
		}
	}


	@Override
	public void execute() throws JSONException, InterruptedException, IOException {

		getProgressReporter().setMax(NUMBER_OF_STEPS);

		log.trace("Updating primary dictionary");

		// Update primary dictionary: load new data, and create mapping.

		getProgressReporter().report(1);

		// Distribute the new IDs among workers
		distributeWorkerResponsibilities(container.entities());

		getProgressReporter().report(1);


		final Import imp = createImport(header, container.getStores(), table.getColumns(), container.size());


		namespace.getStorage().updateImport(imp);

		final Map<Integer, List<String>> buckets2LocalEntities = groupEntitiesByBucket(container.entities(), bucketSize);


		final ColumnStore[] storesSorted = Arrays.stream(table.getColumns())
												 .map(Column::getName)
												 .map(container.getStores()::get)
												 .map(Objects::requireNonNull)
												 .toArray(ColumnStore[]::new);


		log.info("Start sending {} Buckets", buckets2LocalEntities.size());

		// we use this to track assignment to workers.
		final Map<WorkerId, Set<BucketId>> workerAssignments =
				sendBuckets(container.getStarts(), container.getLengths(), imp, buckets2LocalEntities, storesSorted);

		final WorkerHandler handler = namespace.getWorkerHandler();
		workerAssignments.forEach(handler::addBucketsToWorker);

	}

	/**
	 * select, then send buckets.
	 */
	private Map<WorkerId, Set<BucketId>> sendBuckets(Map<String, Integer> starts, Map<String, Integer> lengths, Import imp, Map<Integer, List<String>> buckets2LocalEntities, ColumnStore[] storesSorted) {

		final Map<WorkerId, Set<BucketId>> newWorkerAssignments = new HashMap<>();

		final ProgressReporter subJob = getProgressReporter().subJob(buckets2LocalEntities.size());

		for (Map.Entry<Integer, List<String>> bucket2entities : buckets2LocalEntities.entrySet()) {

			final WorkerInformation responsibleWorker = Objects.requireNonNull(
				namespace
					.getWorkerHandler()
					.getResponsibleWorkerForBucket(bucket2entities.getKey()),
				() -> "No responsible worker for Bucket#" + bucket2entities.getKey());

			awaitFreeJobQueue(responsibleWorker);

			final Bucket bucket =
					selectBucket(starts, lengths, storesSorted, imp, bucket2entities.getKey());

			newWorkerAssignments.computeIfAbsent(responsibleWorker.getId(), (ignored) -> new HashSet<>())
								.add(bucket.getId());

			log.trace("Sending Bucket[{}] to {}", bucket.getId(), responsibleWorker.getId());
			responsibleWorker.send(ImportBucket.forBucket(bucket));

			subJob.report(1);
		}

		return newWorkerAssignments;
	}

	private void awaitFreeJobQueue(WorkerInformation responsibleWorker) {
		try {
			responsibleWorker.getConnectedShardNode().waitForFreeJobQueue();
		}
		catch (InterruptedException e) {
			log.error("Interrupted while waiting for worker[{}] to have free space in queue", responsibleWorker, e);
		}
	}

	/**
	 * - remap Entity-Ids to global
	 * - calculate per-Entity regions of Bucklet (start/end)
	 * - split stores
	 */
	private Bucket selectBucket(Map<String, Integer> localStarts, Map<String, Integer> localLengths, ColumnStore[] stores, Import imp, int bucketId) {

		final int root = bucketSize * bucketId;

		final IntList selectionStart = new IntArrayList();
		final IntList selectionLength = new IntArrayList();
		final Set<String> entities = new HashSet<>(localStarts.keySet());


		// First entity of Bucket starts at 0, the following are appended.
		final Map<String, Integer> entityStarts = new HashMap<>(entities.size());
		final Map<String, Integer> entityEnds = new HashMap<>(entities.size());


		int currentStart = 0;

		for (Map.Entry<String, Integer> entity2Start : localStarts.entrySet()) {
			final String entity = entity2Start.getKey();
			final int start = entity2Start.getValue();

			final int length = localLengths.get(entity);

			selectionStart.add(start);

			selectionLength.add(length);

			entityStarts.put(entity, currentStart);
			entityEnds.put(entity, currentStart + length);

			currentStart += length;
		}

		// copy only the parts of the bucket we need
		final ColumnStore[] bucketStores =
				Arrays.stream(stores)
					  .map(store -> store.select(selectionStart.toIntArray(), selectionLength.toIntArray()))
					  .toArray(ColumnStore[]::new);

		return new Bucket(
				bucketId,
				root,
				selectionLength.intStream().sum(),
				bucketStores,
				entities,
				entityStarts,
				entityEnds,
				imp
		);
	}

	private void distributeWorkerResponsibilities(Set<String> entities) {
		log.debug("Updating bucket assignments.");

		synchronized (namespace) {
			for (String entity : entities) {
				final int bucket = Entity.getBucket(entity, bucketSize);

				if (namespace.getWorkerHandler().getResponsibleWorkerForBucket(bucket) != null) {
					continue;
				}

				namespace.getWorkerHandler().addResponsibility(bucket);
			}
		}
	}


	private Import createImport(PreprocessedHeader header, Map<String, ColumnStore> stores, Column[] columns, int size) {
		final Import imp = new Import(table);

		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());
		imp.setNumberOfEntities(size);

		final ImportColumn[] importColumns = new ImportColumn[columns.length];

		for (int i = 0; i < columns.length; i++) {
			final ColumnStore store = stores.get(columns[i].getName());

			final ImportColumn col = new ImportColumn(imp, store.createDescription(), store.getLines(), store.estimateMemoryConsumptionBytes());

			col.setName(columns[i].getName());

			importColumns[i] = col;
		}

		imp.setColumns(importColumns);

		namespace.getWorkerHandler().sendToAll(new AddImport(imp));
		return imp;
	}


	/**
	 * Group entities by their global bucket id.
	 */
	private Map<Integer, List<String>> groupEntitiesByBucket(Set<String> entities, int bucketSize) {
		return entities.stream()
					   .collect(Collectors.groupingBy(entity -> Entity.getBucket(entity, bucketSize)));

	}


	private Dataset getDataset() {
		return namespace.getDataset();
	}


	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + header.getName();
	}

}
