package com.bakdata.conquery.resources.admin.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
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
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;


@Slf4j
@RequiredArgsConstructor
@Getter
public class AdminDatasetProcessor {

	private final MetaStorage storage; // TODO Remove
	private final ConqueryConfig config;
	private final Validator validator;
	private final DatasetRegistry datasetRegistry;
	private final JobManager jobManager;
	private final IdMutex<DictionaryId> sharedDictionaryLocks = new IdMutex<>();

	/**
	 * Creates and initializes a new dataset if it does not already exist.
	 */
	public synchronized Dataset addDataset(String name) {

		if (datasetRegistry.get(new DatasetId(name)) != null) {
			throw new WebApplicationException("Dataset already exists", Response.Status.CONFLICT);
		}

		// create dataset
		Dataset dataset = new Dataset();
		dataset.setName(name);

		NamespaceStorage datasetStorage = new NamespaceStorage(validator, config.getStorage(), "dataset_" + name);

		datasetStorage.loadData();
		datasetStorage.setMetaStorage(storage);
		datasetStorage.updateDataset(dataset);

		Namespace ns = new Namespace(datasetStorage, config.isFailOnError(), config.configureObjectMapper(Jackson.BINARY_MAPPER.copy()).writerWithView(InternalOnly.class));

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
					Response.Status.CONFLICT);
		}

		namespace.close();
		datasetRegistry.removeNamespace(dataset.getId());
		datasetRegistry.getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(dataset)));

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

		for (int p = 0; p < table.getColumns().length; p++) {
			table.getColumns()[p].setPosition(p);
		}

		namespace.getStorage().addTable(table);
		namespace.sendToAll(new UpdateTable(table));
	}

	/**
	 * Add the concept to the dataset if it does not exist yet.
	 */
	public synchronized void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) {
		concept.setDataset(dataset);
		ValidatorHelper.failOnError(log, validator.validate(concept));

		if (datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Response.Status.CONFLICT);
		}

		addAutomaticSelect(concept, () -> EventDurationSumSelect.create("event_duration_sum"));

		// Register the Concept in the ManagerNode and Workers
		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
		datasetRegistry.get(dataset.getId()).sendToAll(new UpdateConcept(concept));
	}

	/**
	 * Adds some selects to the concept on all levels for convenience.
	 */
	private static void addAutomaticSelect(@NotNull Concept<?> concept, Supplier<UniversalSelect> selectCreator) {
		if (!(concept instanceof TreeConcept)) {
			return;
		}

		// Add to concept
		TreeConcept treeConcept = (TreeConcept) concept;
		final UniversalSelect select = selectCreator.get();
		select.setHolder(treeConcept);
		treeConcept.getSelects().add(select);

		// Add to connectors
		for (ConceptTreeConnector connector : treeConcept.getConnectors()) {
			final UniversalSelect connectorSelect = selectCreator.get();
			connectorSelect.setHolder(connector);
			connector.getSelects().add(connectorSelect);
		}
	}

	/**
	 * Uploads new IdMapping.
	 */
	public void setIdMapping(InputStream data, Namespace namespace) {
		log.info("Received IdMapping for Dataset[{}]", namespace.getDataset().getId());

		CsvParser parser = config.getCsv()
				.withSkipHeader(false)
				.withParseHeaders(false)
				.createParser();

		PersistentIdMap mapping = config.getIdMapping().generateIdMapping(parser.iterate(data).iterator());

		namespace.getStorage().updateIdMapping(mapping);
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

		ImportJob job = ImportJob.create(namespace, inputStream, config.getCluster().getEntityBucketSize(), sharedDictionaryLocks);

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
	 * @implNote This intentionally submits a SlowJob so that it will be queued after all jobs that are already in the queue (usually import jobs).
	 */
	public void updateMatchingStats(Dataset dataset) {
		final Namespace ns = getDatasetRegistry().get(dataset.getId());

		ns.getJobManager().addSlowJob(new SimpleJob("Initiate Update Matching Stats and FilterSearch",
				() -> {
					ns.sendToAll(new UpdateMatchingStatsMessage());
					FilterSearch.updateSearch(getDatasetRegistry(), Collections.singleton(ns.getDataset()), getJobManager(), config.getCsv().createParser());
				}
		));
	}
}
