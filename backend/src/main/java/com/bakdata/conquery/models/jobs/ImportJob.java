package com.bakdata.conquery.models.jobs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the main routine to load data into Conquery.
 */
@Slf4j
@Data
public class ImportJob extends Job {

	private final DistributedNamespace namespace;
	@Getter
	private final Table table;
	private final Import imp;

	private final int bucketId;
	private final PreprocessedData container;


	@Override
	public void execute() throws JSONException, InterruptedException, IOException {

		namespace.getWorkerHandler().assignResponsibleWorker(bucketId);

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
		return "Importing into " + table.getName() + " from " + imp.getName();
	}

}
