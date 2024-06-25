package com.bakdata.conquery.mode.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.messages.namespaces.specific.StartCalculateCblocks;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler of {@link Import} requests that realizes them both on the manager and the cluster's shards.
 */
@AllArgsConstructor
@Slf4j
public class ClusterImportHandler implements ImportHandler {

	private final DatasetRegistry<DistributedNamespace> datasetRegistry;

	@SneakyThrows
	@Override
	public void updateImport(Namespace namespace, InputStream inputStream) {
		handleImport(namespace, inputStream, true);
	}

	private static void handleImport(Namespace namespace, InputStream inputStream, boolean update) throws IOException {
		try (PreprocessedReader parser = new PreprocessedReader(inputStream, namespace.getPreprocessMapper())) {
			// We parse semi-manually as the incoming file consist of multiple documents we read progressively:
			// 1) the header to check metadata
			// 2...) The chunked Buckets

			final PreprocessedHeader header = parser.readHeader();

			final Table table = validateImportable(((DistributedNamespace) namespace), header, update);

			readAndDistributeImport(((DistributedNamespace) namespace), table, header, parser);

			clearDependentConcepts(namespace.getStorage().getAllConcepts(), table);
		}
	}

	/**
	 * Handle validity and update logic.
	 */
	private static Table validateImportable(DistributedNamespace namespace, PreprocessedHeader header, boolean update) {
		final TableId tableId = new TableId(namespace.getDataset().getId(), header.getTable());
		final ImportId importId = new ImportId(tableId, header.getName());

		final Table table = namespace.getStorage().getTable(tableId);

		if (table == null) {
			throw new BadRequestException("Table[%s] does not exist.".formatted(tableId));
		}

		// Ensure that Import and Table have the same schema
		final List<String> errors = header.assertMatch(table);

		if (!errors.isEmpty()) {
			final String errorsMessage = String.join("\n - ", errors);

			log.error("Problems concerning Import `{}`:\n{}", importId, errorsMessage);
			throw new BadRequestException("Headers[%s] do not match Table[%s]:\n%s".formatted(importId, table.getId(), errorsMessage));
		}

		final Import processedImport = namespace.getStorage().getImport(importId);

		if (update) {
			if (processedImport == null) {
				throw new NotFoundException("Import[%s] is not present.".formatted(importId));
			}

			// before updating the import, make sure that all workers removed the prior import
			namespace.getWorkerHandler().sendToAll(new RemoveImportJob(processedImport));
			namespace.getStorage().removeImport(importId);
		}
		else if (processedImport != null) {
			throw new WebApplicationException("Import[%s] is already present.".formatted(importId), Response.Status.CONFLICT);
		}

		return table;
	}

	private static void readAndDistributeImport(DistributedNamespace namespace, Table table, PreprocessedHeader header, PreprocessedReader reader) {
		final TableId tableId = new TableId(namespace.getDataset().getId(), header.getTable());
		final ImportId importId = new ImportId(tableId, header.getName());

		log.info("BEGIN importing {} into {}", header.getName(), table);

		Import imp = null;

		final List<Collection<String>> collectedEntities =  new ArrayList<>();

		for (PreprocessedData container : (Iterable<? extends PreprocessedData>) () -> reader) {

			if (imp == null) {
				// We need a container to create a description.
				imp = header.createImportDescription(table, container.getStores());

				namespace.getWorkerHandler().sendToAll(new AddImport(imp));
				namespace.getStorage().updateImport(imp);
			}

			log.trace("DONE reading bucket {}.{}, contains {} entities.", importId, container.getBucketId(), container.size());

			final Bucket bucket = Bucket.fromPreprocessed(table, container, imp);

			final WorkerInformation responsibleWorker = namespace.getWorkerHandler().assignResponsibleWorker(bucket.getId());

			sendBucket(bucket, responsibleWorker);

			// NOTE: I want the bucket to be GC'd as early as possible, so I just store the part(s) I need later.

			collectedEntities.add(bucket.entities());
		}

		namespace.getJobManager().addSlowJob(
				new RegisterImportEntities(collectedEntities, namespace, importId)
		);

		log.debug("Successfully read {} Buckets, containing {} entities for {}", header.getNumberOfBuckets(), header.getNumberOfEntities(), importId);

		namespace.getWorkerHandler().sendUpdatedWorkerInformation();

	}

	private static void clearDependentConcepts(Collection<Concept<?>> allConcepts, Table table) {
		for (Concept<?> c : allConcepts) {
			for (Connector con : c.getConnectors()) {
				if (!con.getTable().equals(table)) {
					continue;
				}

				con.getConcept().clearMatchingStats();
			}
		}
	}

	/**
	 * select, then send buckets.
	 */
	public static WorkerId sendBucket(Bucket bucket, WorkerInformation responsibleWorker) {

		responsibleWorker.awaitFreeJobQueue();

		log.trace("Sending Bucket[{}] to {}", bucket.getId(), responsibleWorker.getId());
		responsibleWorker.send(new ImportBucket(bucket.getId().toString(), bucket));

		return responsibleWorker.getId();
	}

	@SneakyThrows
	@Override
	public void addImport(Namespace namespace, InputStream inputStream) {
		handleImport(namespace, inputStream, false);
	}

	@Override
	public void deleteImport(Import imp) {

		final DatasetId id = imp.getTable().getDataset().getId();
		final DistributedNamespace namespace = datasetRegistry.get(id);

		clearDependentConcepts(namespace.getStorage().getAllConcepts(), imp.getTable());

		namespace.getStorage().removeImport(imp.getId());
		namespace.getWorkerHandler().sendToAll(new RemoveImportJob(imp));

		// Remove bucket assignments for consistency report
		namespace.getWorkerHandler().removeBucketAssignmentsForImportFormWorkers(imp);
	}

	@Override
	public void calculateCBlocks(Namespace namespace) {
		namespace.getJobManager().addSlowJob(
				new Job() {
					@Override
					public void execute() {
						((DistributedNamespace) namespace).getWorkerHandler().sendToAll(new StartCalculateCblocks());
					}

					@Override
					public String getLabel() {
						return "Initiate calculateCBlocksJob";
					}
				}
		);
	}

}
