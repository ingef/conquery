package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.jobs.ImportJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.*;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.google.common.base.Strings;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@Getter
public class AdminDatasetProcessor {

	private final MetaStorage storage; // TODO Remove
	private final ConqueryConfig config;
	private final Validator validator;
	@Nullable
	private final String storagePrefix;
	private final DatasetRegistry datasetRegistry;
	private final JobManager jobManager;


	public synchronized Dataset addDataset(String name) throws JSONException {
		// create dataset
		Dataset dataset = new Dataset();
		dataset.setName(name);


		final List<String> pathName = Strings.isNullOrEmpty(storagePrefix)
				? List.of("dataset_" + name)
				: List.of(storagePrefix, "dataset_" + name);

		NamespaceStorage datasetStorage = new NamespaceStorage(validator, config.getStorage(), pathName);

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

	public synchronized void deleteDataset(Dataset dataset) {
		final Namespace namespace = datasetRegistry.get(dataset.getId());

		if (!namespace.getStorage().getTables().isEmpty()) {
			throw new IllegalArgumentException(
					String.format(
							"Cannot delete dataset `%s`, because it still has tables: `%s`",
							dataset.getId(),
							namespace.getStorage().getTables().stream()
									.map(Table::getId)
									.map(Objects::toString)
									.collect(Collectors.joining(","))
					));
		}

		namespace.close();
		datasetRegistry.removeNamespace(dataset.getId());
		datasetRegistry.getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(dataset)));

	}

	public synchronized void addSecondaryId(Namespace namespace, SecondaryIdDescription secondaryId) {
		final Dataset dataset = namespace.getDataset();
		secondaryId.setDataset(dataset);

		log.info("Received new SecondaryId[{}]", secondaryId.getId());

		namespace.getStorage().addSecondaryId(secondaryId);

		namespace.sendToAll(new UpdateSecondaryId(secondaryId));
	}

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

	public synchronized void addTable(@NonNull Table table, Namespace namespace) throws JSONException {
		Dataset dataset = namespace.getDataset();

		if (table.getDataset() == null) {
			table.setDataset(dataset);
		}
		else if (!table.getDataset().equals(dataset)) {
			throw new IllegalArgumentException();
		}

		ValidatorHelper.failOnError(log, validator.validate(table));

		for (int p = 0; p < table.getColumns().length; p++) {
			table.getColumns()[p].setPosition(p);
		}


		namespace.getStorage().addTable(table);
		namespace.sendToAll(new UpdateTable(table));
	}



	public synchronized void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) throws JSONException {
		concept.setDataset(dataset);
		ValidatorHelper.failOnError(log, validator.validate(concept));
		// Register the Concept in the ManagerNode and Workers
		if (datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Response.Status.CONFLICT);
		}

		addAutomaticSelect(concept, () -> EventDurationSumSelect.create("event_duration_sum"));

		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
		datasetRegistry.get(dataset.getId()).sendToAll(new UpdateConcept(concept));
	}

	private static void addAutomaticSelect(@NotNull Concept<?> concept, Supplier<UniversalSelect> selectCreator) {
		if (concept instanceof TreeConcept) {
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
	}


	public void setIdMapping(InputStream data, Namespace namespace) throws JSONException, IOException {
		CsvParser parser = config.getCsv()
				.withSkipHeader(false)
				.withParseHeaders(false)
				.createParser();

		PersistentIdMap mapping = config.getIdMapping().generateIdMapping(parser.iterate(data).iterator());

		namespace.getStorage().updateIdMapping(mapping);
	}


	public void setStructure(Dataset dataset, StructureNode[] structure) throws JSONException {
		datasetRegistry.get(dataset.getId()).getStorage().updateStructure(structure);
	}




	@SneakyThrows
	public void addImport(Namespace namespace, InputStream inputStream) throws IOException {

		ImportJob.create(namespace, inputStream, config.getCluster().getEntityBucketSize())
				.ifPresent(job -> namespace.getJobManager().addSlowJob(job));
	}

	public synchronized void deleteImport(Import imp) {
		final Namespace namespace = datasetRegistry.get(imp.getTable().getDataset().getId());


		namespace.getStorage().removeImport(imp.getId());
		namespace.sendToAll(new RemoveImportJob(imp));

		// Remove bucket assignments for consistency report
		namespace.removeBucketAssignmentsForImportFormWorkers(imp);
	}

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

	public synchronized void deleteConcept(Concept<?> concept) {
		final Namespace namespace = datasetRegistry.get(concept.getDataset().getId());

		namespace.getStorage().removeConcept(concept.getId());
		getJobManager()
				.addSlowJob(new SimpleJob("sendToAll: remove " + concept.getId(), () -> namespace.sendToAll(new RemoveConcept(concept))));
	}

	public void updateMatchingStats(Dataset dataset) {
		final Namespace ns = getDatasetRegistry().get(dataset.getId());

		ns.getJobManager().addSlowJob(new SimpleJob("Initiate Update Matching Stats and FilterSearch",
				() -> {
					ns.sendToAll(new UpdateMatchingStatsMessage());
					FilterSearch.updateSearch(getDatasetRegistry(), Collections.singleton(ns.getDataset()), getJobManager(), config.getCsv().createParser());
				}
		));
	}




	public void updateMatchingStats(DatasetId datasetId) {
		final Namespace ns = getDatasetRegistry().get(datasetId);

		ns.getJobManager().addSlowJob(new SimpleJob("Initiate Update Matching Stats and FilterSearch",
				() -> {
					ns.sendToAll(new UpdateMatchingStatsMessage());
					FilterSearch.updateSearch(getDatasetRegistry(), Collections.singleton(ns.getDataset()), getJobManager(), config.getCsv().createParser());
				}
		));
	}
}
