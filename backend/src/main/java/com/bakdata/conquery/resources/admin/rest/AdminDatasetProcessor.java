package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
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
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedDictionaries;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
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

import javax.annotation.Nullable;
import javax.validation.Validator;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

		Namespace ns = new Namespace(datasetStorage, config.isFailOnError());

		datasetRegistry.add(ns);

		// for now we just add one worker to every ShardNode
		for (ShardNodeInformation node : datasetRegistry.getShardNodes().values()) {
			addWorker(node, dataset);
		}

		return dataset;
	}

	private void addWorker(ShardNodeInformation node, Dataset dataset) {
		node.send(new AddWorker(dataset));
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

	public synchronized void deleteSecondaryId(SecondaryIdDescription secondaryId) {
		final Namespace namespace = datasetRegistry.get(secondaryId.getDataset().getId());

		// Before we commit this deletion, we check if this SecondaryId still has dependent Columns.
		final List<Column> dependents = namespace.getStorage().getTables().stream()
				.map(Table::getColumns).flatMap(Arrays::stream)
				.filter(column -> column.getSecondaryId() != null)
				.filter(column -> column.getSecondaryId().getId().equals(secondaryId))
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

		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
		datasetRegistry.get(dataset.getId()).sendToAll(new UpdateConcept(concept));
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
	public void addImport(Namespace namespace, String importSource, InputStream inputStream) throws IOException {

		final Dataset ds = namespace.getDataset();
		final PreprocessedHeader header;
		try(PreprocessedReader parser = new PreprocessedReader(inputStream)){

			parser.addReplacement(Dataset.PLACEHOLDER.getId(), ds);

			// We parse semi-manually as the incoming file consist of multiple documents we only read progressively:
			// 1) the header to check metadata
			// 2) The Dictionaries to be imported and transformed
			// 3) The ColumnStores themselves which contain references to the previously imported dictionaries.

			header = parser.readHeader();

			final TableId tableId = new TableId(ds.getId(), header.getTable());
			Table table = namespace.getStorage().getTable(tableId);

			if(table == null){
				throw new BadRequestException(String.format("Table[%s] does not exist.", tableId));
			}

			final ImportId importId = new ImportId(table.getId(), header.getName());

			if (namespace.getStorage().getImport(importId) != null) {
				throw new WebApplicationException(String.format("Import[%s] is already present.", importId), Response.Status.CONFLICT);
			}
			log.trace("Begin reading Dictionaries");

			PreprocessedDictionaries dictionaries = parser.readDictionaries();

			Map<DictionaryId, Dictionary> normalDictionaries = collectNormalDictionaries(ds, dictionaries.getDictionaries(), table.getColumns(), header.getName());

			// The following call is synchronized, because the
			Map<String, DictionaryMapping> sharedDictionaryMappings = importSharedDictionaries(namespace, dictionaries.getDictionaries(), table.getColumns(), header.getName());


			// We inject the mappings into the parser, so that the incoming placeholder names are replaced with the new names of the dictionaries. This allows us to use NsIdRef in conjunction with shared-Dictionaries
			parser.addAllReplacements(normalDictionaries);

			for (DictionaryMapping value : sharedDictionaryMappings.values()) {
				parser.addReplacement(new DictionaryId(Dataset.PLACEHOLDER.getId(), value.getSourceDictionary().getName()), value.getTargetDictionary());
			}

			log.trace("Begin reading data.");

			PreprocessedData container = parser.readData();

			if (container.isEmpty()) {
				log.warn("Import was empty. Skipping.");
				return;
			}

			log.info("Importing {}", importSource);

			final ImportJob job = new ImportJob(
					datasetRegistry.get(ds.getId()),
					table,
					importSource,
					config.getCluster().getEntityBucketSize(),
					header,
					dictionaries.getPrimaryDictionary(),
					normalDictionaries,
					sharedDictionaryMappings,
					container
			);

			datasetRegistry.get(ds.getId()).getJobManager().addSlowJob(job);
		}


	}
	/**
	 * Collects all dictionaries that map only to columns of this import.
	 */
	private static synchronized Map<DictionaryId, Dictionary> collectNormalDictionaries(Dataset dataset, Map<String, Dictionary> dicts, Column[] columns, String importName) {

		// Empty Maps are Coalesced to null by Jackson
		if (dicts == null) {
			return Collections.emptyMap();
		}

		final Map<DictionaryId, Dictionary> out = new HashMap<>();

		log.trace("Importing Normal Dictionaries.");

		for (Column column : columns) {

			if (column.getType() != MajorTypeId.STRING || column.getSharedDictionary() != null) {
				continue;
			}

			// Might not have an underlying Dictionary (eg Singleton, direct-Number)
			// but could also be an error :/ Most likely the former
			if (!dicts.containsKey(column.getName()) || dicts.get(column.getName()) == null) {
				log.trace("No Dictionary for {}", column);
				continue;
			}

			final Dictionary dict = dicts.get(column.getName());
			final String name = computeDefaultDictionaryName(importName, column);


			out.put(new DictionaryId(Dataset.PLACEHOLDER.getId(), dict.getName()), dict);


			dict.setDataset(dataset);
			dict.setName(name);
		}


		return out;
	}
	/**
	 * Import shared Dictionaries, create new Dictionary if not already present. Create mappings from incoming to already present dict.
	 */
	private static  synchronized Map<String, DictionaryMapping> importSharedDictionaries(Namespace namespace, Map<String, Dictionary> dicts, Column[] columns, String importName) {

		// Empty Maps are Coalesced to null by Jackson
		if (dicts == null) {
			return Collections.emptyMap();
		}

		final Map<String, DictionaryMapping> out = new HashMap<>();

		log.trace("Importing Shared Dictionaries");

		for (Column column : columns) {

			if (column.getSharedDictionary() == null) {
				continue;
			}

			// Might not have an underlying Dictionary (eg Singleton, direct-Number)
			// but could also be an error :/ Most likely the former
			if (!dicts.containsKey(column.getName()) || dicts.get(column.getName()) == null) {
				log.trace("No Dictionary for {}", column);
				continue;
			}

			final String sharedDictionaryId = computeSharedDictionaryName(column);
			final Dictionary dictionary = dicts.get(column.getName());

			log.debug("Column[{}.{}] part of shared Dictionary[{}]", importName, column.getName(), sharedDictionaryId);

			final Dataset dataset = namespace.getDataset();
			final Dictionary targetDictionary = namespace.getStorage().getDictionary(new DictionaryId(dataset.getId(), sharedDictionaryId));

			log.debug("Merging into shared Dictionary[{}]", targetDictionary);

			DictionaryMapping mapping = null;
			if (targetDictionary == null) {
				mapping = DictionaryMapping.create(dictionary, new MapDictionary(dataset, sharedDictionaryId));
			}
			else {
				mapping = extendSharedDictionary(dataset,dictionary, sharedDictionaryId, targetDictionary);
			}

			// We need to update the storages for now in this synchronized part
			namespace.getStorage().updateDictionary(mapping.getTargetDictionary());
			namespace.sendToAll(new UpdateDictionary(mapping.getTargetDictionary()));

			out.put(column.getName(), mapping);

		}

		return out;
	}

	private static String computeSharedDictionaryName(Column column) {
		return column.getSharedDictionary();
	}

	private static DictionaryMapping extendSharedDictionary(Dataset dataset, Dictionary incoming, String targetDictionary, Dictionary targetDict) {

		log.debug("Merging into shared Dictionary[{}]", targetDictionary);

		if (targetDict == null) {
			targetDict = new MapDictionary(dataset, targetDictionary);
		}

		targetDict = Dictionary.copyUncompressed(targetDict);

		DictionaryMapping mapping = DictionaryMapping.create(incoming, targetDict);

		targetDict.setName(targetDictionary);

		return mapping;
	}

	private static String computeDefaultDictionaryName(String importName, Column column) {
		return String.format("%s#%s", importName, column.getId().toString());
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
