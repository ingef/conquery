package com.bakdata.conquery.models.jobs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedDictionaries;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

/**
 * This is the main routine to load data into Conquery.
 */
@RequiredArgsConstructor
@Slf4j
public class ImportJob extends Job {

	private final Namespace namespace;

	private final Table table;
	private final String importSource;
	private final int bucketSize;
	private final PreprocessedHeader header;
	private final Dictionary primaryDictionary;
	private final Map<DictionaryId, Dictionary> normalDictionaries;
	private final Map<String, DictionaryMapping> sharedDictionaryMappings;
	private final PreprocessedData container;

	private static final int NUMBER_OF_STEPS = /* directly in execute = */4;

	@Override
	public void execute() throws JSONException, InterruptedException, IOException {

		getProgressReporter().setMax(NUMBER_OF_STEPS);

		log.trace("Updating primary dictionary");

		// Update primary dictionary: load new data, and create mapping.
		final DictionaryMapping primaryMapping = importPrimaryDictionary(primaryDictionary);

		getProgressReporter().report(1);

		// Distribute the new IDs among workers
		distributeWorkerResponsibilities(primaryMapping);

		// Persist Dictionaries on manager and shards
		for (Dictionary dict : normalDictionaries.values()) {
			log.trace("Sending {} to all Workers", dict);
			namespace.getStorage().updateDictionary(dict);
			namespace.sendToAll(new UpdateDictionary(dict));
		}



		getProgressReporter().report(1);

		log.debug("Done reading data. Contains {} Entities.", container.size());


		log.info("Remapping Dictionaries {}", sharedDictionaryMappings.values());

		applyDictionaryMappings(sharedDictionaryMappings, container.getStores());


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

		workerAssignments.forEach(namespace::addBucketsToWorker);

		getProgressReporter().done();
	}

	/**
	 * select, then send buckets.
	 */
	private Map<WorkerId, Set<BucketId>> sendBuckets(Map<Integer, Integer> starts, Map<Integer, Integer> lengths, DictionaryMapping primaryMapping, Import imp, Map<Integer, List<Integer>> buckets2LocalEntities, ColumnStore[] storesSorted)
			throws JsonProcessingException {

		Map<WorkerId, Set<BucketId>> newWorkerAssignments = new HashMap<>();

		final ProgressReporter subJob = getProgressReporter().subJob(buckets2LocalEntities.size());

		for (Map.Entry<Integer, List<Integer>> bucket2entities : buckets2LocalEntities.entrySet()) {

			WorkerInformation responsibleWorker =
					Objects.requireNonNull(namespace.getResponsibleWorkerForBucket(bucket2entities.getKey()), () -> "No responsible worker for Bucket#"
																													+ bucket2entities.getKey());

			awaitFreeJobQueue(responsibleWorker);

			final Bucket bucket =
					selectBucket(starts, lengths, storesSorted, primaryMapping, imp, bucket2entities.getKey(), bucket2entities.getValue());

			newWorkerAssignments.computeIfAbsent(responsibleWorker.getId(), (ignored) -> new HashSet<>())
								.add(bucket.getId());

			log.trace("Sending Bucket[{}] to {}", bucket.getId(), responsibleWorker.getId());
			responsibleWorker.send(ImportBucket.forBucket(bucket));

			subJob.report(1);
		}

		subJob.done();

		return newWorkerAssignments;
	}

	private void awaitFreeJobQueue(WorkerInformation responsibleWorker) {
		try {
			responsibleWorker.getConnectedShardNode().waitForFreeJobqueue();
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
	public Bucket selectBucket(Map<Integer, Integer> starts, Map<Integer, Integer> lengths, ColumnStore[] stores, DictionaryMapping primaryMapping, Import imp, int bucketId, List<Integer> bucketEntities) {

		int[] globalIds = bucketEntities.stream().mapToInt(primaryMapping::source2Target).toArray();

		int[] selectionStart = bucketEntities.stream().mapToInt(starts::get).toArray();
		int[] entityLengths = bucketEntities.stream().mapToInt(lengths::get).toArray();

		// First entity of Bucket starts at 0, the following are appended.
		int[] entityStarts = Arrays.copyOf(entityLengths, entityLengths.length);
		entityStarts[0] = 0;
		for (int index = 1; index < entityLengths.length; index++) {
			entityStarts[index] = entityStarts[index - 1] + entityLengths[index - 1];
		}

		// copy only the parts of the bucket we need
		final ColumnStore[] bucketStores =
				Arrays.stream(stores)
					  .map(store -> store.select(selectionStart, entityLengths))
					  .toArray(ColumnStore[]::new);


		return new Bucket(
				bucketId,
				Arrays.stream(entityLengths).sum(),
				bucketStores,
				new Int2IntArrayMap(globalIds, entityStarts),
				new Int2IntArrayMap(globalIds, entityLengths),
				imp
		);
	}

	private DictionaryMapping importPrimaryDictionary(Dictionary primaryDictionary) {

		final DictionaryId dictionaryId = ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset());

		Dictionary orig = namespace.getStorage().getDictionary(dictionaryId);

		// Start with an empty Dictionary and merge into it
		if (orig == null) {
			log.trace("No prior Dictionary[{}], creating one", dictionaryId);
			orig = new MapDictionary(getDataset(), dictionaryId.getName());
		}

		Dictionary primaryDict = Dictionary.copyUncompressed(orig);

		DictionaryMapping primaryMapping = DictionaryMapping.create(primaryDictionary, primaryDict);
		log.debug("Mapped {} new ids", primaryMapping.getNumberOfNewIds());

		//if no new ids we shouldn't recompress and store
		if (primaryMapping.getNumberOfNewIds() == 0) {
			log.trace("No new ids");
			return primaryMapping;
		}

		namespace.getStorage()
				 .updateDictionary(primaryDict);

		return primaryMapping;
	}

	public void distributeWorkerResponsibilities(DictionaryMapping primaryMapping) {
		log.debug("Updating bucket assignments.");

		synchronized (namespace) {
			for (int entity : primaryMapping.getSource2TargetMap()) {
				int bucket = Entity.getBucket(entity, bucketSize);

				if (namespace.getResponsibleWorkerForBucket(bucket) != null) {
					continue;
				}

				namespace.addResponsibility(bucket);
			}

			// While we hold the lock on the namespace distribute the new, consistent state among the workers
			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new UpdateWorkerBucket(w));
			}
		}
	}





	/**
	 * Apply new positions into incoming shared dictionaries.
	 */
	public void applyDictionaryMappings(Map<String, DictionaryMapping> mappings, Map<String, ColumnStore> values) {
		final ProgressReporter subJob = getProgressReporter().subJob(mappings.size());

		for (Map.Entry<String, DictionaryMapping> entry : mappings.entrySet()) {
			final String columnName = entry.getKey();
			final DictionaryMapping mapping = entry.getValue();

			final StringStore stringStore = (StringStore) values.get(columnName);

			log.debug("Remapping Column[{}] = {} with {}", columnName, stringStore, mapping);

			// we need to find a new Type for the index-Column as it's going to be remapped and might change in size
			final IntegerParser indexParser = new IntegerParser(new ParserConfig());

			final IntSummaryStatistics statistics = Arrays.stream(mapping.getSource2TargetMap()).summaryStatistics();

			indexParser.setLines(stringStore.getLines());
			indexParser.setMinValue(statistics.getMin());
			indexParser.setMaxValue(statistics.getMax());

			final IntegerStore newType = indexParser.findBestType();

			log.trace("Decided for {}", newType);

			mapping.applyToStore(stringStore, newType);

			stringStore.setIndexStore(newType);
			subJob.report(1);
		}
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
		namespace.sendToAll(new AddImport(imp));
		return imp;
	}


	/**
	 * Group entities by their global bucket id.
	 */
	public Map<Integer, List<Integer>> groupEntitiesByBucket(Set<Integer> entities, DictionaryMapping primaryMapping, int bucketSize) {
		return entities.stream()
					   .collect(Collectors.groupingBy(entity -> Entity.getBucket(primaryMapping.source2Target(entity), bucketSize)));

	}


	private Dataset getDataset() {
		return namespace.getDataset();
	}


	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importSource;
	}

}
