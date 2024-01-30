package com.bakdata.conquery.models.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
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
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedDictionaries;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.ResourceUtil;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
	private final PreprocessedDictionaries dictionaries;
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
			Table table = namespace.getStorage().getTable(tableId);

			if (table == null) {
				throw new BadRequestException(String.format("Table[%s] does not exist.", tableId));
			}

			// Ensure that Import and Table have the same schema
			header.assertMatch(table);

			final ImportId importId = new ImportId(table.getId(), header.getName());
			Import processedImport = namespace.getStorage().getImport(importId);

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


			log.trace("Begin reading Dictionaries");
			parser.addReplacement(Dataset.PLACEHOLDER.getId(), ds);
			PreprocessedDictionaries dictionaries = parser.readDictionaries();

			Map<DictionaryId, Dictionary>
					dictReplacements =
					createLocalIdReplacements(dictionaries.getDictionaries(), table, header.getName(), namespace.getStorage(), sharedDictionaryLocks);

			// We inject the mappings into the parser, so that the incoming placeholder names are replaced with the new names of the dictionaries. This allows us to use NsIdRef in conjunction with shared-Dictionaries
			parser.addAllReplacements(dictReplacements);

			log.trace("Begin reading data.");

			PreprocessedData container = parser.readData();

			log.debug("Done reading data. Contains {} Entities.", container.size());

			log.info("Importing {} into {}", header.getName(), tableId);

			return new ImportJob(
					namespace,
					table,
					entityBucketSize,
					header,
					dictionaries,
					container,
					config,
					sharedDictionaryLocks
			);
		}
	}

	/**
	 * Collects all dictionaries that map only to columns of this import.
	 */
	private static Map<DictionaryId, Dictionary> createLocalIdReplacements(Map<String, Dictionary> dicts, Table table, String importName, NamespaceStorage storage, IdMutex<DictionaryId> sharedDictionaryLocks) {

		// Empty Maps are Coalesced to null by Jackson
		if (dicts == null) {
			return Collections.emptyMap();
		}

		final Map<DictionaryId, Dictionary> out = new HashMap<>();

		log.trace("Importing Normal Dictionaries.");

		for (Column column : table.getColumns()) {

			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			// Might not have an underlying Dictionary (eg Singleton, direct-Number)
			// but could also be an error :/ Most likely the former
			if (!dicts.containsKey(column.getName()) || dicts.get(column.getName()) == null) {
				log.trace("No Dictionary for {}", column);
				continue;
			}

			if (column.getSharedDictionary() != null) {
				column.createSharedDictionaryReplacement(dicts, storage, out, sharedDictionaryLocks);
			}
			else {
				// Its a normal dictionary (only valid for this column in this import)
				column.createSingleColumnDictionaryReplacement(dicts, importName, out);
			}
		}

		return out;
	}

	/**
	 * Import all dictionaries. Shared dictionaries are merge into existing ones. All are distributed to corresponding workers.
	 * Create mappings for shared dictionaries dict.
	 * This is not synchronized because the methods is called within the job execution.
	 */
	private static Map<String, DictionaryMapping> importDictionaries(DistributedNamespace namespace, Map<String, Dictionary> dicts, Column[] columns, String importName, Table table, IdMutex<DictionaryId> sharedDictionaryLocks) {

		// Empty Maps are Coalesced to null by Jackson
		if (dicts == null) {
			return Collections.emptyMap();
		}

		final Map<String, DictionaryMapping> out = new ConcurrentHashMap<>();

		log.debug("BEGIN importing {} Dictionaries", dicts.size());

		// Might not have an underlying Dictionary (eg Singleton, direct-Number)
		// but could also be an error :/ Most likely the former
		// It's a shared dictionary
		// This should never fail, becaus the dictionary is pre-created in the replacement generation step

		Arrays.stream(columns)
			  .parallel()
			  .filter(column -> column.getType() == MajorTypeId.STRING)
			  .filter(col -> col.getSharedDictionary() == null)
			  .map(col -> dicts.get(col.getName()))
			  .filter(Objects::nonNull)
			  .forEach(dictionary -> {
				  // Normal Dictionary -> no merge necessary, just distribute
				  storeAndDistributeDictionary(namespace, dictionary);
			  });

		// We group by sharedDictionary to avoid sending dictionaries multliple times
		Arrays.stream(columns)
			  .parallel()
			  .filter(column -> column.getType() == MajorTypeId.STRING)
			  .filter(col -> col.getSharedDictionary() != null)
			  .filter(col -> dicts.containsKey(col.getName()))
			  .collect(Collectors.groupingBy(Column::getSharedDictionary))
			  .values()
			  .forEach(allColumns -> {
				  final Column refColumn = allColumns.get(0);
				  final String sharedDictionaryName = refColumn.getSharedDictionary();
				  final DictionaryId dictionaryId = new DictionaryId(namespace.getDataset().getId(), sharedDictionaryName);

				  log.debug("Column[{}.{}.{}] part of shared Dictionary[{}]", table.getId(), importName, refColumn.getName(), sharedDictionaryName);

				  // We have to lock here, as sibling columns might both use the same shared-dictionary
				  try (IdMutex.Locked lock = sharedDictionaryLocks.acquire(dictionaryId)) {
					  final Dictionary sharedDictionary = namespace.getStorage().getDictionary(dictionaryId);

					  ResourceUtil.throwNotFoundIfNull(dictionaryId, sharedDictionary);
					  log.trace("Merging into shared Dictionary[{}]", sharedDictionary);

					  int newIds = 0;

					  for (Column column : allColumns) {
						  final Dictionary importDictionary = dicts.get(column.getName());

						  final DictionaryMapping mapping = DictionaryMapping.createAndImport(importDictionary, sharedDictionary);

						  newIds += mapping.getNumberOfNewIds();
						  out.put(refColumn.getName(), mapping);
					  }

					  if (newIds > 0) {
						  storeAndDistributeDictionary(namespace, sharedDictionary);
					  }
				  }
			  });
		return out;
	}

	private static void storeAndDistributeDictionary(DistributedNamespace namespace, Dictionary dictionary) {
		log.trace("Sending {} to all Workers", dictionary);
		namespace.getStorage().updateDictionary(dictionary);
		namespace.getWorkerHandler().sendToAll(new UpdateDictionary(dictionary));
	}


	@Override
	public void execute() throws JSONException, InterruptedException, IOException {

		getProgressReporter().setMax(NUMBER_OF_STEPS);

		log.trace("Updating primary dictionary");

		// Update primary dictionary: load new data, and create mapping.
		final DictionaryMapping primaryMapping = importPrimaryDictionary(dictionaries.getPrimaryDictionary());

		getProgressReporter().report(1);

		// Distribute the new IDs among workers
		distributeWorkerResponsibilities(primaryMapping);

		getProgressReporter().report(1);


		log.info("Importing Dictionaries");

		Map<String, DictionaryMapping> sharedDictionaryMappings =
				importDictionaries(namespace, dictionaries.getDictionaries(), table.getColumns(), header.getName(), table, sharedDictionaryLocks);

		log.info("Remapping Dictionaries {}", sharedDictionaryMappings.values());

		remapToSharedDictionary(sharedDictionaryMappings, container.getStores());


		Import imp = createImport(header, container.getStores(), table.getColumns(), container.size());


		namespace.getStorage().updateImport(imp);

		Map<Integer, List<Integer>> buckets2LocalEntities = groupEntitiesByBucket(container.entities(), primaryMapping, bucketSize);


		final ColumnStore[] storesSorted = Arrays.stream(table.getColumns())
												 .map(Column::getName)
												 .map(container.getStores()::get)
												 .map(Objects::requireNonNull)
												 .toArray(ColumnStore[]::new);


		log.info("Start sending {} Buckets", buckets2LocalEntities.size());

		// we use this to track assignment to workers.
		final Map<WorkerId, Set<BucketId>> workerAssignments =
				sendBuckets(container.getStarts(), container.getLengths(), primaryMapping, imp, buckets2LocalEntities, storesSorted);

		WorkerHandler handler = namespace.getWorkerHandler();
		workerAssignments.forEach(handler::addBucketsToWorker);

	}

	/**
	 * select, then send buckets.
	 */
	private Map<WorkerId, Set<BucketId>> sendBuckets(Map<Integer, Integer> starts, Map<Integer, Integer> lengths, DictionaryMapping primaryMapping, Import imp, Map<Integer, List<Integer>> buckets2LocalEntities, ColumnStore[] storesSorted) {

		Map<WorkerId, Set<BucketId>> newWorkerAssignments = new HashMap<>();

		final ProgressReporter subJob = getProgressReporter().subJob(buckets2LocalEntities.size());

		for (Map.Entry<Integer, List<Integer>> bucket2entities : buckets2LocalEntities.entrySet()) {

			WorkerInformation responsibleWorker = Objects.requireNonNull(
				namespace
					.getWorkerHandler()
					.getResponsibleWorkerForBucket(bucket2entities.getKey()),
				() -> "No responsible worker for Bucket#" + bucket2entities.getKey());

			awaitFreeJobQueue(responsibleWorker);

			final Bucket bucket =
					selectBucket(starts, lengths, storesSorted, primaryMapping, imp, bucket2entities.getKey(), bucket2entities.getValue());

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
	private Bucket selectBucket(Map<Integer, Integer> localStarts, Map<Integer, Integer> localLengths, ColumnStore[] stores, DictionaryMapping primaryMapping, Import imp, int bucketId, List<Integer> localEntities) {

		final int root = bucketSize * bucketId;


		IntList selectionStart = new IntArrayList();
		IntList selectionLength = new IntArrayList();
		IntSet entities = new IntOpenHashSet();


		// First entity of Bucket starts at 0, the following are appended.
		int[] entityStarts = new int[bucketSize];
		int[] entityEnds = new int[bucketSize];

		Arrays.fill(entityEnds, -1);
		Arrays.fill(entityStarts, -1);

		int currentStart = 0;

		for (int position = 0; position < bucketSize; position++) {
			int globalId = root + position;

			int localId = primaryMapping.target2Source(globalId);

			if (localId == -1) {
				continue;
			}

			if (!localStarts.containsKey(localId)) {
				continue;
			}

			entities.add(globalId);

			final int length = localLengths.get(localId);

			selectionStart.add(localStarts.get(localId));

			selectionLength.add(length);

			entityStarts[position] = currentStart;
			entityEnds[position] = currentStart + length;

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

	private DictionaryMapping importPrimaryDictionary(Dictionary primaryDictionary) {


		Dictionary orig = namespace.getStorage().getPrimaryDictionaryRaw();

		Dictionary primaryDict = Dictionary.copyUncompressed(orig);

		DictionaryMapping primaryMapping = DictionaryMapping.createAndImport(primaryDictionary, primaryDict);

		log.debug("Mapped {} new ids", primaryMapping.getNumberOfNewIds());

		//if no new ids we shouldn't recompress and store
		if (primaryMapping.getNumberOfNewIds() == 0) {
			log.trace("No new ids");
			return primaryMapping;
		}

		namespace.getStorage()
				 .updatePrimaryDictionary(primaryDict);

		return primaryMapping;
	}

	private void distributeWorkerResponsibilities(DictionaryMapping primaryMapping) {
		log.debug("Updating bucket assignments.");

		synchronized (namespace) {
			for (int entity : primaryMapping.target()) {
				int bucket = Entity.getBucket(entity, bucketSize);

				if (namespace.getWorkerHandler().getResponsibleWorkerForBucket(bucket) != null) {
					continue;
				}

				namespace.getWorkerHandler().addResponsibility(bucket);
			}
		}
	}


	/**
	 * Apply new positions into incoming shared dictionaries.
	 */
	private void remapToSharedDictionary(Map<String, DictionaryMapping> mappings, Map<String, ColumnStore> values) {

		if (mappings.isEmpty()) {
			log.trace("No columns with shared dictionary appear to be in the import.");
			return;
		}

		final ProgressReporter subJob = getProgressReporter().subJob(mappings.size());

		// we need to find a new Type for the index-Column as it's going to be remapped and might change in size
		mappings.entrySet().parallelStream()
				.forEach(entry -> {
					final String columnName = entry.getKey();
					final DictionaryMapping mapping = entry.getValue();

					final StringStore stringStore = (StringStore) values.get(columnName);
					log.debug("Remapping Column[{}] = {} with {}", columnName, stringStore, mapping);
					final IntegerParser indexParser = new IntegerParser(config);
					final IntSummaryStatistics statistics = mapping.target().intStream().summaryStatistics();

					indexParser.setLines(stringStore.getLines());
					indexParser.setMinValue(statistics.getMin());
					indexParser.setMaxValue(statistics.getMax());

					final IntegerStore newType = indexParser.findBestType();

					log.trace("Decided for {}", newType);

					mapping.applyToStore(stringStore, newType);
					stringStore.setIndexStore(newType);

					subJob.report(1);
				});
	}

	private Import createImport(PreprocessedHeader header, Map<String, ColumnStore> stores, Column[] columns, int size) {
		Import imp = new Import(table);

		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());
		imp.setNumberOfEntities(size);

		final ImportColumn[] importColumns = new ImportColumn[columns.length];

		for (int i = 0; i < columns.length; i++) {
			final ColumnStore store = stores.get(columns[i].getName());

			ImportColumn col = new ImportColumn(imp, store.createDescription(), store.getLines(), store.estimateMemoryConsumptionBytes());

			col.setName(columns[i].getName());

			importColumns[i] = col;
		}

		imp.setColumns(importColumns);

		Set<DictionaryId> dictionaries = new HashSet<>();

		for (Column column : columns) {
			// only non-shared dictionaries need to be registered here
			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			// shared dictionaries are not related to a specific import.
			if (column.getSharedDictionary() != null) {
				continue;
			}

			// Some StringStores don't have Dictionaries.
			final StringStore stringStore = (StringStore) stores.get(column.getName());

			if (!stringStore.isDictionaryHolding()) {
				continue;
			}

			dictionaries.add(stringStore.getUnderlyingDictionary().getId());
		}

		imp.setDictionaries(dictionaries);
		namespace.getWorkerHandler().sendToAll(new AddImport(imp));
		return imp;
	}


	/**
	 * Group entities by their global bucket id.
	 */
	private Map<Integer, List<Integer>> groupEntitiesByBucket(Set<Integer> entities, DictionaryMapping primaryMapping, int bucketSize) {
		return entities.stream()
					   .collect(Collectors.groupingBy(entity -> Entity.getBucket(primaryMapping.source2Target(entity), bucketSize)));

	}


	private Dataset getDataset() {
		return namespace.getDataset();
	}


	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + header.getName();
	}

}
