package com.bakdata.conquery.resources.admin.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.jobs.ImportJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveTable;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateTable;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@Getter
public class AdminDatasetProcessor {


	public static final int MAX_IMPORTS_TEXT_LENGTH = 100;
	private static final String ABBREVIATION_MARKER = "\u2026";

	private final MetaStorage storage; // TODO Remove
	private final ConqueryConfig config;
	private final Validator validator;
	private final DatasetRegistry datasetRegistry;
	private final JobManager jobManager;
	private final IdMutex<DictionaryId> sharedDictionaryLocks = new IdMutex<>();

	/**
	 * Creates and initializes a new dataset if it does not already exist.
	 */
	public synchronized Dataset addDataset(Dataset dataset) {

		final String name = dataset.getName();
		if (datasetRegistry.get(new DatasetId(name)) != null) {
			throw new WebApplicationException("Dataset already exists", Response.Status.CONFLICT);
		}

		NamespaceStorage datasetStorage = new NamespaceStorage(validator, "dataset_" + name);

		datasetStorage.openStores(config.getStorage());
		datasetStorage.loadData();
		datasetStorage.setMetaStorage(storage);
		datasetStorage.updateDataset(dataset);
		datasetStorage.updateIdMapping(new EntityIdMap());

		Namespace ns =
				new Namespace(
						datasetRegistry, datasetStorage, config.isFailOnError(),
						config.configureObjectMapper(Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER))
							  .writerWithView(InternalOnly.class),
						config.getCsv()
				);


		datasetRegistry.add(ns);

		// for now we just add one worker to every ShardNode
		for (ShardNodeInformation node : datasetRegistry.getShardNodes().values()) {
			node.send(new AddWorker(dataset));
		}

		return dataset;
	}

	/**
	 * Delete dataset if it is empty.
	 */
	public synchronized void deleteDataset(Dataset dataset) {
		final Namespace namespace = datasetRegistry.get(dataset.getId());

		if (!namespace.getStorage().getTables().isEmpty()) {
			throw new WebApplicationException(
					String.format(
							"Cannot delete dataset `%s`, because it still has tables: `%s`",
							dataset.getId(),
							namespace.getStorage().getTables().stream()
									 .map(Table::getId)
									 .map(Objects::toString)
									 .collect(Collectors.joining(","))
					),
					Response.Status.CONFLICT
			);
		}

		datasetRegistry.removeNamespace(dataset.getId());

	}

	/**
	 * Add SecondaryId if it doesn't already exist.
	 */
	public synchronized void addSecondaryId(Namespace namespace, SecondaryIdDescription secondaryId) {
		final Dataset dataset = namespace.getDataset();
		secondaryId.setDataset(dataset);

		if (namespace.getStorage().getSecondaryId(secondaryId.getId()) != null) {
			throw new WebApplicationException("SecondaryId already exists", Response.Status.CONFLICT);
		}

		log.info("Received new SecondaryId[{}]", secondaryId.getId());

		namespace.getStorage().addSecondaryId(secondaryId);

		namespace.sendToAll(new UpdateSecondaryId(secondaryId));
	}

	/**
	 * Delete SecondaryId if it does not have any dependents.
	 */
	public synchronized void deleteSecondaryId(@NonNull SecondaryIdDescription secondaryId) {
		final Namespace namespace = datasetRegistry.get(secondaryId.getDataset().getId());

		// Before we commit this deletion, we check if this SecondaryId still has dependent Columns.
		final List<Column> dependents = namespace.getStorage().getTables().stream()
												 .map(Table::getColumns).flatMap(Arrays::stream)
												 .filter(column -> secondaryId.equals(column.getSecondaryId()))
												 .collect(Collectors.toList());

		if (!dependents.isEmpty()) {
			final Set<TableId> tables = dependents.stream().map(Column::getTable).map(Identifiable::getId).collect(Collectors.toSet());
			log.error(
					"SecondaryId[{}] still present on {}",
					secondaryId,
					tables
			);

			throw new ForbiddenException(String.format("SecondaryId still has dependencies. %s", tables));
		}

		log.info("Deleting SecondaryId[{}]", secondaryId);

		namespace.getStorage().removeSecondaryId(secondaryId.getId());
		namespace.sendToAll(new RemoveSecondaryId(secondaryId));
	}

	/**
	 * Add table to Dataset if it doesn't already exist.
	 */
	@SneakyThrows
	public synchronized void addTable(@NonNull Table table, Namespace namespace) {
		Dataset dataset = namespace.getDataset();

		if (table.getDataset() == null) {
			table.setDataset(dataset);
		}
		else if (!table.getDataset().equals(dataset)) {
			throw new IllegalArgumentException();
		}


		if (namespace.getStorage().getTable(table.getId()) != null) {
			throw new WebApplicationException("Table already exists", Response.Status.CONFLICT);
		}

		ValidatorHelper.failOnError(log, validator.validate(table));

		namespace.getStorage().addTable(table);
		namespace.sendToAll(new UpdateTable(table));
	}


	/**
	 * update a concept of the given dataset.
	 * Therefore the concept will be deleted first then added
	 */
	public synchronized void updateConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) {
		concept.setDataset(dataset);
		if (!datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new NotFoundException("Can't find the concept in the dataset " + concept.getId());
		}
		//deletes the old content of the concept using his id
		deleteConcept(concept);

		//adds new content of the content
		addConcept(dataset, concept);
	}

	/**
	 * Add the concept to the dataset if it does not exist yet
	 */
	public synchronized void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) {
		concept.setDataset(dataset);
		ValidatorHelper.failOnError(log, validator.validate(concept));

		if (datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Response.Status.CONFLICT);
		}
		final Namespace namespace = datasetRegistry.get(concept.getDataset().getId());


		// Register the Concept in the ManagerNode and Workers
		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
		getJobManager().addSlowJob(new SimpleJob(String.format("sendToAll : Add %s ", concept.getId()), () -> namespace.sendToAll(new UpdateConcept(concept))));
	}

	/**
	 * Uploads new IdMapping.
	 */
	public void setIdMapping(InputStream data, Namespace namespace) {
		log.info("Received IdMapping for Dataset[{}]", namespace.getDataset().getId());

		CsvParser parser = config.getCsv()
								 .withSkipHeader(false)
								 .withParseHeaders(true)
								 .createParser();

		try {

			parser.beginParsing(data);

			EntityIdMap mapping = EntityIdMap.generateIdMapping(parser, config.getFrontend().getQueryUpload().getIds());
			namespace.getStorage().updateIdMapping(mapping);

		}
		finally {
			parser.stopParsing();
		}
	}

	/**
	 * Uploads new Structure for namespace.
	 */
	public void setStructure(Namespace namespace, StructureNode[] structure) {
		log.info("Add Structure for Dataset[{}]", namespace.getDataset().getId());

		namespace.getStorage().updateStructure(structure);
	}


	/**
	 * Reads an Import partially Importing it if not yet present, then submitting it for full import.
	 */
	@SneakyThrows
	public void addImport(Namespace namespace, InputStream inputStream) throws IOException {

		ImportJob job = ImportJob.createOrUpdate(namespace, inputStream, config.getCluster().getEntityBucketSize(), sharedDictionaryLocks, config, false);
		namespace.getJobManager().addSlowJob(job);
	}

	/**
	 * Reads an Import partially Importing it if it is present, then submitting it for full import [Update of an import].
	 */
	@SneakyThrows
	public void updateImport(Namespace namespace, InputStream inputStream) throws IOException {

		ImportJob job = ImportJob.createOrUpdate(namespace, inputStream, config.getCluster().getEntityBucketSize(), sharedDictionaryLocks, config, true);
		namespace.getJobManager().addSlowJob(job);
	}

	/**
	 * Deletes an import.
	 */
	public synchronized void deleteImport(Import imp) {
		final Namespace namespace = datasetRegistry.get(imp.getTable().getDataset().getId());

		namespace.getStorage().removeImport(imp.getId());
		namespace.sendToAll(new RemoveImportJob(imp));

		// Remove bucket assignments for consistency report
		namespace.removeBucketAssignmentsForImportFormWorkers(imp);
	}

	/**
	 * Deletes a table if it has no dependents or not forced to do so.
	 */
	public synchronized List<ConceptId> deleteTable(Table table, boolean force) {
		final Namespace namespace = datasetRegistry.get(table.getDataset().getId());

		final List<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts().stream().flatMap(c -> c.getConnectors().stream())
															.filter(con -> con.getTable().equals(table))
															.map(Connector::getConcept)
															.collect(Collectors.toList());

		if (force || dependentConcepts.isEmpty()) {
			for (Concept<?> concept : dependentConcepts) {
				deleteConcept(concept);
			}

			namespace.getStorage().getAllImports().stream()
					 .filter(imp -> imp.getTable().equals(table))
					 .forEach(this::deleteImport);

			namespace.getStorage().removeTable(table.getId());
			namespace.sendToAll(new RemoveTable(table));
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	/**
	 * Deletes a concept.
	 */
	public synchronized void deleteConcept(Concept<?> concept) {
		final Namespace namespace = datasetRegistry.get(concept.getDataset().getId());

		namespace.getStorage().removeConcept(concept.getId());
		getJobManager()
				.addSlowJob(new SimpleJob("sendToAll: remove " + concept.getId(), () -> namespace.sendToAll(new RemoveConcept(concept))));
	}

	/**
	 * Issues all Shards to do an UpdateMatchingStats.
	 *
	 * @implNote This intentionally submits a SlowJob so that it will be queued after all jobs that are already in the queue (usually import jobs).
	 */
	public void updateMatchingStats(Dataset dataset) {
		final Namespace ns = getDatasetRegistry().get(dataset.getId());

		ns.getJobManager().addSlowJob(new SimpleJob(
				"Initiate Update Matching Stats and FilterSearch",
				() -> {

					ns.sendToAll(new UpdateMatchingStatsMessage());
					ns.getFilterSearch().updateSearch();
				}
		));
	}

	public EntityIdMap getIdMapping(Namespace namespace) {
		return namespace.getStorage().getIdMapping();
	}
}
