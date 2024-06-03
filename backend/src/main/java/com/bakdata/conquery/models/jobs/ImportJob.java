package com.bakdata.conquery.models.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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
	private final Import imp;

	private final int bucketId;
	private final PreprocessedHeader header;
	private final PreprocessedData container;

	public static Table createOrUpdate(DistributedNamespace namespace, InputStream inputStream, int entityBucketSize, ConqueryConfig config, boolean update)
			throws IOException {

		try (PreprocessedReader parser = new PreprocessedReader(inputStream, namespace.getPreprocessMapper())) {

			final Dataset ds = namespace.getDataset();

			// We parse semi-manually as the incoming file consist of multiple documents we only read progressively:
			// 1) the header to check metadata
			// 3) The chunked Buckets


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

			Import imp = null;

			for (PreprocessedData container : (Iterable<? extends PreprocessedData>) () -> parser){

				if (imp == null) {
					// We need a container to create a description.
					imp = createImport(header, container.getStores(), table.getColumns(), table);

					namespace.getWorkerHandler().sendToAll(new AddImport(imp));
					namespace.getStorage().updateImport(imp);
				}

				log.trace("Done reading data. Contains {} Entities.", container.size());

				log.info("Importing {} into {}", header.getName(), tableId);

				final ImportJob importJob = new ImportJob(
						namespace,
						table,
						imp,
						container.getBucketId(),
						header,
						container
				);

				namespace.getJobManager().addSlowJob(importJob);
			}

			return table;
		}
	}

	private static Import createImport(PreprocessedHeader header, Map<String, ColumnStore> stores, Column[] columns, Table table) {
		final Import imp = new Import(table);

		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());
		imp.setNumberOfEntities(header.getNumberOfEntities());

		final ImportColumn[] importColumns = new ImportColumn[columns.length];

		for (int i = 0; i < columns.length; i++) {
			final ColumnStore store = stores.get(columns[i].getName());

			final ImportColumn col = new ImportColumn(imp, store.createDescription(), store.getLines(), store.estimateMemoryConsumptionBytes());

			col.setName(columns[i].getName());

			importColumns[i] = col;
		}

		imp.setColumns(importColumns);

		return imp;
	}

	@Override
	public void execute() throws JSONException, InterruptedException, IOException {


		log.trace("Updating primary dictionary");

		assignResponsibleWorker();

		final ColumnStore[] storesSorted = sortColumns(table, container.getStores());

		log.info("Start sending {} Bucket", bucketId);

		final Bucket bucket =
				new Bucket(bucketId, storesSorted, new Object2IntOpenHashMap<>(container.getStarts()), new Object2IntOpenHashMap<>(container.getEnds()), imp);

		for (String entity : bucket.entities()) {
			namespace.getStorage().assignEntityBucket(entity, bucketId);
		}

		// we use this to track assignment to workers.
		final WorkerId workerAssignments = sendBucket(bucket);

		namespace.getWorkerHandler().addBucketsToWorker(workerAssignments, Set.of(bucket.getId()));

	}

	private void assignResponsibleWorker() {
		log.debug("Updating bucket assignments.");

		synchronized (namespace) {

			if (namespace.getWorkerHandler().getResponsibleWorkerForBucket(bucketId) != null) {
				return;
			}

			namespace.getWorkerHandler().addResponsibility(bucketId);
		}
	}

	private static ColumnStore[] sortColumns(Table table, Map<String, ColumnStore> stores) {
		final ColumnStore[] storesSorted =
				Arrays.stream(table.getColumns())
					  .map(Column::getName)
					  .map(stores::get)
					  .map(Objects::requireNonNull)
					  .toArray(ColumnStore[]::new);
		return storesSorted;
	}

	/**
	 * select, then send buckets.
	 */
	private WorkerId sendBucket(Bucket bucket) {

		final WorkerInformation responsibleWorker = Objects.requireNonNull(
				namespace
						.getWorkerHandler()
						.getResponsibleWorkerForBucket(bucketId),
				() -> "No responsible worker for Bucket#" + bucketId
		);

		awaitFreeJobQueue(responsibleWorker);

		log.trace("Sending Bucket[{}] to {}", bucket.getId(), responsibleWorker.getId());
		responsibleWorker.send(new ImportBucket(bucket.getId().toString(), bucket));

		return responsibleWorker.getId();
	}

	private void awaitFreeJobQueue(WorkerInformation responsibleWorker) {
		try {
			responsibleWorker.getConnectedShardNode().waitForFreeJobQueue();
		}
		catch (InterruptedException e) {
			log.error("Interrupted while waiting for worker[{}] to have free space in queue", responsibleWorker, e);
		}
	}


	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + header.getName();
	}

}
