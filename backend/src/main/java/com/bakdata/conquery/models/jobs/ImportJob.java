package com.bakdata.conquery.models.jobs;

import java.io.File;
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
import java.util.stream.Collectors;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.parser.specific.IntegerParser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.Preprocessed;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;
import com.jakewharton.byteunits.BinaryByteUnit;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ImportJob extends Job {

	private final ObjectReader headerReader = Jackson.BINARY_MAPPER.readerFor(PreprocessedHeader.class);

	private final Namespace namespace;
	private final TableId tableId;
	private final File importFile;
	private final int bucketSize;


	@Override
	public void execute() throws JSONException, InterruptedException {
		final PreprocessedData container;
		final PreprocessedHeader header;

		try (HCFile file = new HCFile(importFile, false)) {
			if (log.isInfoEnabled()) {
				log.info("Reading HCFile {}: header size: {}  content size: {}", importFile, BinaryByteUnit.format(file.getHeaderSize()), BinaryByteUnit.format(file.getContentSize()));
			}

			header = readHeader(file);
			log.info("Importing {} into {}", header.getName(), tableId);

			//check that all workers are connected
			namespace.checkConnections();

			//import the actual data
			log.info("Begin reading data.");

			try (InputStream in = file.readContent()) {
				container = Preprocessed.readContainer(in);
			}

		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to load the file " + importFile, exception);
		}

		final Map<Integer, Integer> starts = container.getStarts();
		log.info("Done reading data. Contains {} Entities.", starts.size());

		if (container.isEmpty()) {
			log.warn("Import was empty. Skipping.");
			return;
		}

		final Table table = namespace.getStorage().getDataset().getTables().get(this.tableId);
		final Map<String,ColumnStore<?>> stores = container.getValues();


		// Update primary dictionary: load new data, and create mapping.
		DictionaryMapping primaryMapping = importPrimaryDictionary(container.getPrimaryDictionary());

		// Distribute the new IDs among workers
		distributeWorkerResponsibilities(primaryMapping);


		final Map<String, DictionaryMapping> mappings = importDictionaries(container.getDictionaries(), table.getColumns(), header.getName(), stores);

		setDictionaryIds(stores, table.getColumns(), header.getName());

		applyDictionaryMappings(mappings, stores, table.getColumns());


		//create data import and store/send it

		Import outImport = createImport(header, stores, table.getColumns());

		namespace.getStorage().updateImport(outImport);
		namespace.sendToAll(new AddImport(outImport));


		Map<Integer, List<Integer>> buckets2LocalEntities = groupEntitiesByBucket(starts.keySet(), primaryMapping, bucketSize);


		// per incoming bucket:
		final ColumnStore<?>[] storesSorted = Arrays.stream(table.getColumns())
												 .map(Column::getName)
												 .map(stores::get)
												 .peek(Objects::requireNonNull)
												 .toArray(ColumnStore[]::new);

		for (Map.Entry<Integer, List<Integer>> bucket2entities : buckets2LocalEntities.entrySet()) {
			final Bucket bucket =
					selectBucket(starts, container.getLengths(), storesSorted, primaryMapping, outImport.getId(), bucket2entities.getKey(), bucket2entities.getValue());
			sendBucket(bucket);
		}

	}

	/**
	 * - remap Entity-Ids to global
	 * - calculate per-Entity regions (start/end)
	 * - split stores
	 * - send store to responsible workers
	 */
	public Bucket selectBucket(Map<Integer, Integer> starts, Map<Integer, Integer> lengths, ColumnStore<?>[] stores, DictionaryMapping primaryMapping, ImportId importId, int bucketId, List<Integer> bucketEntities) {

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
		final ColumnStore<?>[] bucketStores =
				Arrays.stream(stores)
					  .map(store -> store.select(selectionStart, entityLengths))
					  .toArray(ColumnStore<?>[]::new);


		return new Bucket(
				bucketId,
				importId,
				Arrays.stream(entityLengths).sum(),
				bucketStores,
				new Int2IntArrayMap(globalIds, entityStarts),
				new Int2IntArrayMap(globalIds, entityLengths),
				globalIds.length
		);
	}

	private PreprocessedHeader readHeader(HCFile file) throws JsonParseException, IOException {
		try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
			PreprocessedHeader header = headerReader.readValue(in);


			Table tab = namespace.getStorage().getDataset().getTables().getOrFail(tableId);

			header.assertMatch(tab);

			return header;
		}
	}

	private DictionaryMapping importPrimaryDictionary(Dictionary underlyingDictionary) {
		log.info("Updating primary dictionary");

		final DictionaryId dictionaryId = ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset());

		Dictionary orig = namespace.getStorage().getDictionary(dictionaryId);

		// Start with an empty Dictionary and merge into it
		if (orig == null) {
			log.debug("No prior Dictionary[{}], creating one", dictionaryId);
			orig = new MapDictionary(namespace.getDataset().getId(), dictionaryId.getDictionary());
		}

		Dictionary primaryDict = Dictionary.copyUncompressed(orig);

		log.debug("Map values");

		DictionaryMapping primaryMapping = DictionaryMapping.create(underlyingDictionary, primaryDict);

		//if no new ids we shouldn't recompress and store
		if (primaryMapping.getNumberOfNewIds() == 0) {
			log.debug("no new ids");
		}
		//but if there are new ids we have to
		else {
			log.debug("{} new ids", primaryMapping.getNumberOfNewIds());

			namespace.getStorage().updateDictionary(primaryDict);
		}
		return primaryMapping;
	}

	public void distributeWorkerResponsibilities(DictionaryMapping primaryMapping) {
		log.info("Updating bucket assignments.");

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

	private Map<String, DictionaryMapping> importDictionaries(Map<String, Dictionary> dicts, Column[] columns, String importName, Map<String, ColumnStore<?>> stores)
			throws JSONException {

		// Empty Maps are Coalesced to null by Jackson
		if(dicts == null){
			return Collections.emptyMap();
		}

		final Map<String, DictionaryMapping> out = new HashMap<>();

		log.debug("Import contains Dictionaries = {}", dicts);

		for (int i = 0; i < columns.length; i++) {
			Column column = columns[i];
			//if the column uses a shared dictionary we have to merge the existing dictionary into that

			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			// Might not have an underlying Dictionary (eg Singleton, direct-Number)
			// but could also be an error :/ Most likely the former
			if (!dicts.containsKey(column.getName()) || dicts.get(column.getName()) == null) {
				log.trace("No Dictionary for {}", column);
				continue;
			}

			// if the target column has a shared dictionary, we merge them and then update the merged dictionary.
			if (column.getSharedDictionary() != null) {
				final DictionaryId sharedDictionaryId = computeSharedDictionaryId(column);
				final Dictionary dictionary = dicts.get(column.getName());

				log.info("Column[{}.{}] = `{}` part of shared Dictionary[{}]", importName, column.getName(), stores.get(column.getName()), sharedDictionaryId);

				final DictionaryMapping mapping = importSharedDictionary(dictionary, sharedDictionaryId);

				out.put(column.getName(), mapping);

				continue;
			}


			//store external infos into master and slaves
			final Dictionary dict = dicts.get(column.getName());
			final DictionaryId dictionaryId = computeDefaultDictionaryId(importName, column);

			try {
				dict.setDataset(dictionaryId.getDataset());
				dict.setName(dictionaryId.getDictionary());

				log.info("Sending {} to all Workers", dict);
				namespace.getStorage().updateDictionary(dict);
				namespace.sendToAll(new UpdateDictionary(dict));
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to store dictionary " + dict, e);
			}
		}

		return out;
	}

	public void setDictionaryIds(Map<String, ColumnStore<?>> values, Column[] columns, String importName) {
		for (int i = 0; i < columns.length; i++) {
			Column column = columns[i];

			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			final StringType stringType = (StringType) values.get(column.getName());

			// if not shared use default naming
			if (column.getSharedDictionary() != null) {
				stringType.setUnderlyingDictionary(computeSharedDictionaryId(column));
			}
			else {
				stringType.setUnderlyingDictionary(computeDefaultDictionaryId(importName, column));
			}
		}
	}

	public void applyDictionaryMappings(Map<String, DictionaryMapping> mappings, Map<String, ColumnStore<?>> values, Column[] columns) {
		for (Column column : columns) {
			if (column.getType() != MajorTypeId.STRING || column.getSharedDictionary() == null) {
				continue;
			}

			// apply mapping
			final DictionaryMapping mapping = mappings.get(column.getName());

			final StringType stringType = (StringType) values.get(column.getName());

			if(mapping == null){
				if(stringType.getUnderlyingDictionary() != null) {
					throw new IllegalStateException(String.format("Missing mapping for %s", column));
				}

				continue;
			}

			log.debug("Remapping Column[{}] = {} with {}", column.getId(), stringType, mapping);


			// we need to find a new Type for the index-Column as it's going to be remapped and might change in size
			final IntegerParser indexParser = new IntegerParser();

			final IntSummaryStatistics statistics = Arrays.stream(mapping.getSource2TargetMap()).summaryStatistics();

			indexParser.setLines(stringType.getLines());
			indexParser.setNullLines(stringType.getNullLines());
			indexParser.setMinValue(statistics.getMin());
			indexParser.setMaxValue(statistics.getMax());

			final ColumnStore<Long> newType = indexParser.findBestType();

			log.debug("Decided for {}", newType);

			mapping.applyToStore(stringType, newType, stringType.getLines());

			stringType.setIndexStore(newType);
		}
	}

	private Import createImport(PreprocessedHeader header, Map<String, ColumnStore<?>> stores, Column[] columns) {
		Import imp = new Import(tableId);

		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());

		final ImportColumn[] importColumns = new ImportColumn[columns.length];

		for (int i = 0; i < columns.length; i++) {
			final ColumnStore<?> store = stores.get(columns[i].getName());

			ImportColumn col = new ImportColumn(imp, store.createDescription());

			col.setName(columns[i].getName());

			importColumns[i]  = col;
		}

		imp.setColumns(importColumns);

		Set<DictionaryId> dictionaries = new HashSet<>();

		for (Column column : columns) {
			// only non-shared dictionaries need to be registered here
			// shared dictionaries are not related to a specific import.
			if (column.getType() != MajorTypeId.STRING && column.getSharedDictionary() != null) {
				continue;
			}

			dictionaries.add(computeDefaultDictionaryId(header.getName(), column));
		}

		imp.setDictionaries(dictionaries);
		return imp;
	}



	/**
	 * Group entities by their global bucket id.
	 */
	public Map<Integer, List<Integer>> groupEntitiesByBucket(Set<Integer> entities, DictionaryMapping primaryMapping, int bucketSize) {
		return entities.stream()
					   .collect(Collectors.groupingBy(entity -> Entity.getBucket(primaryMapping.source2Target(entity), bucketSize)));

	}

	private void sendBucket(Bucket bucket) {
		int bucketNumber = bucket.getBucket();

		WorkerInformation responsibleWorker = namespace.getResponsibleWorkerForBucket(bucketNumber);
		if (responsibleWorker == null) {
			throw new IllegalStateException("No responsible worker for bucket " + bucketNumber);
		}
		// todo send this async

		try {
			responsibleWorker.getConnectedShardNode().waitForFreeJobqueue();
		}
		catch (InterruptedException e) {
			log.error("Interrupted while waiting for worker[{}] to have free space in queue", responsibleWorker, e);
		}

		responsibleWorker.send(new ImportBucket(bucket));
	}

	private DictionaryId computeSharedDictionaryId(Column column) {
		return new DictionaryId(namespace.getDataset().getId(), column.getSharedDictionary());
	}

	private DictionaryMapping importSharedDictionary(Dictionary incoming, DictionaryId targetDictionary) throws JSONException {

		log.info("merging into shared Dictionary[{}]", targetDictionary);

		Dictionary shared = namespace.getStorage().getDictionary(targetDictionary);

		if (shared == null) {
			shared = new MapDictionary(targetDictionary.getDataset(), targetDictionary.getDictionary());
		}
		shared = Dictionary.copyUncompressed(shared);

		DictionaryMapping mapping = DictionaryMapping.create(incoming, shared);

		shared.setName(targetDictionary.getDictionary());
		shared.setDataset(targetDictionary.getDataset());

		namespace.getStorage().updateDictionary(shared);
		namespace.sendToAll(new UpdateDictionary(shared));

		return mapping;
	}

	public DictionaryId computeDefaultDictionaryId(String importName, Column column) {
		return new DictionaryId(namespace.getDataset().getId(), String.format("%s#%s", importName, column.getId().toString()));
	}

	@Override
	public String getLabel() {
		return "Importing into " + tableId + " from " + importFile;
	}

}
