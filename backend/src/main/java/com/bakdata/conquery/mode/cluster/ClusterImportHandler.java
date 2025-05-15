package com.bakdata.conquery.mode.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.future.WriteFuture;

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

			try(Stream<Concept<?>> allConcepts = namespace.getStorage().getAllConcepts();) {
				clearDependentConcepts(allConcepts, table.getId());
			}
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
			throw new BadRequestException("Headers[%s] do not match Table[%s]:\n%s".formatted(importId, tableId, errorsMessage));
		}

		final Import processedImport = namespace.getStorage().getImport(importId);

		if (update) {
			if (processedImport == null) {
				throw new NotFoundException("Import[%s] is not present.".formatted(importId));
			}

			// before updating the import, make sure that all workers removed the prior import
			namespace.getWorkerHandler().sendToAll(new RemoveImportJob(importId));
			namespace.getStorage().removeImport(importId);
		}
		else if (processedImport != null) {
			throw new WebApplicationException("Import[%s] is already present.".formatted(importId), Response.Status.CONFLICT);
		}

		return table;
	}

	private static void readAndDistributeImport(DistributedNamespace namespace, Table table, PreprocessedHeader header, PreprocessedReader reader) {
		final TableId tableId = table.getId();

		log.info("BEGIN importing {} into {}", header.getName(), table);

		Import imp = null;

		final Map<Integer, Collection<String>> collectedEntities = new HashMap<>();

		for (PreprocessedData container : (Iterable<? extends PreprocessedData>) () -> reader) {

			if (imp == null) {
				// We need a container to create a description.
				imp = header.createImportDescription(tableId, container.getStores());

				namespace.getWorkerHandler().sendToAll(new AddImport(imp));
				namespace.getStorage().updateImport(imp);
			}


			final Bucket bucket = Bucket.fromPreprocessed(table, container, imp);

			final BucketId bucketId = bucket.getId();
			log.trace("DONE reading bucket `{}`, contains {} entities.", bucketId, bucket.entities().size());

			final WorkerInformation responsibleWorker = namespace.getWorkerHandler().assignResponsibleWorker(bucketId);

			sendBucket(bucket, responsibleWorker).addListener((f) ->  {
				if(((WriteFuture)f).isWritten()) {
					log.trace("Sent Bucket {}", bucketId);
					return;
				}
				log.warn("Failed to send Bucket {}", bucketId);
			});

			// NOTE: I want the bucket to be GC'd as early as possible, so I just store the part(s) I need later.
			collectedEntities.put(bucket.getBucket(), bucket.entities());
		}

		if (imp == null){
			log.warn("Import {} is not empty.", header.getName());
			return;
		}

		namespace.getJobManager().addSlowJob(new RegisterImportEntities(collectedEntities, namespace, imp.getId()));

		log.debug("Successfully read {} Buckets, containing {} entities for `{}`", header.getNumberOfBuckets(), header.getNumberOfEntities(), imp.getId());

		namespace.getWorkerHandler().sendUpdatedWorkerInformation();

	}

	private static void clearDependentConcepts(Stream<Concept<?>> allConcepts, TableId table) {
		allConcepts.map(Concept::getConnectors)
				   .flatMap(List::stream)
				   .filter(con -> con.resolveTableId().equals(table))
				   .map(Connector::getConcept)
				   .forEach(Concept::clearMatchingStats);
	}

	/**
	 * select, then send buckets.
	 */
	public static WriteFuture sendBucket(Bucket bucket, WorkerInformation responsibleWorker) {

		log.trace("Sending Bucket[{}] to {}", bucket.getId(), responsibleWorker.getId());
		return responsibleWorker.send(new ImportBucket(bucket.getId(), bucket));

	}

	@SneakyThrows
	@Override
	public void addImport(Namespace namespace, InputStream inputStream) {
		handleImport(namespace, inputStream, false);
	}

	@Override
	public void deleteImport(ImportId imp) {

		final DatasetId id = imp.getTable().getDataset();
		final DistributedNamespace namespace = datasetRegistry.get(id);

		try(Stream<Concept<?>> allConcepts = namespace.getStorage().getAllConcepts()) {
			clearDependentConcepts(allConcepts, imp.getTable());
		}

		namespace.getStorage().removeImport(imp);
		namespace.getWorkerHandler().sendToAll(new RemoveImportJob(imp));

		// Remove bucket assignments for consistency report
		namespace.getWorkerHandler().removeBucketAssignmentsForImportFormWorkers(imp);
	}

}
