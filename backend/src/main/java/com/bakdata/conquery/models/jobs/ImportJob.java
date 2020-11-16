package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.Preprocessed;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.string.StringType;
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
	private final TableId table;
	private final File importFile;
	private final int bucketSize;


	@Override
	public void execute() throws JSONException {
		try (HCFile file = new HCFile(importFile, false)) {

			if (log.isInfoEnabled()) {
				log.info("Reading HCFile {}: header size: {}  content size: {}", importFile, BinaryByteUnit.format(file.getHeaderSize()), BinaryByteUnit.format(file.getContentSize()));
			}

			PreprocessedHeader header = readHeader(file);
			log.info("Importing {} into {}", header.getName(), table);

			//check that all workers are connected
			namespace.checkConnections();

			//import the actual data
			log.info("Begin reading data.");
			final Preprocessed.DataContainer container;
			try (InputStream in = new GZIPInputStream(file.readContent())) {
				container = Preprocessed.readContainer(in);
			}


			if (container.getStarts() == null) {
				log.warn("Import was empty. Skipping.");
				return;
			}

			log.info("Done reading data. Contains {} Entities.", container.getStarts().size());


			final Table tableDescription = namespace.getStorage().getDataset().getTables().get(this.table);

			final CType<?, ?>[] stores = container.getValues();

			// todo use a constant
			DictionaryMapping primaryMapping = importPrimaryDictionary(container.getDictionaries().get("primary_dictionary"));
			//update primary dictionary

			final Map<String, DictionaryMapping> mappings = importDictionaries(header, container.getDictionaries());

			CType<?, ?>[] values = container.getValues();
			setDictionaryIds(header, tableDescription, values);

			applyDictionaryMappings(tableDescription, mappings, values);


			for (CType<?, ?> column : container.getValues()) {
				// todo if we import the dictionaries first, then load them into the columns and remap them we don't need this
				// todo for that we need to map the relation from column to dict better, which also allows us to do the mapping
				column.loadExternalInfos(dict -> namespace.getStorage().getDictionary(dict));
			}


			// Distribute the new IDs between the slaves
			log.debug("partition new IDs");

			// Allocate a responsibility for all yet unassigned buckets.
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

			//create data import and store/send it
			log.info("updating import information");
			//if mapping is not required we can also use the old infos here
			Import outImport = createImport(header, stores);


			namespace.getStorage().updateImport(outImport);
			namespace.sendToAll(new AddImport(outImport));


			// but first remap String values

			Map<Integer, List<Integer>> buckets2LocalEntities = groupByBucket(container.getStarts().keySet(), primaryMapping, bucketSize);


			for (Map.Entry<Integer, List<Integer>> bucket2entities : buckets2LocalEntities.entrySet()) {

				int currentBucket = bucket2entities.getKey();
				final List<Integer> entities = bucket2entities.getValue();

				int[] globalIds = entities.stream().mapToInt(primaryMapping::source2Target).toArray();

				int[] selectionStart = entities.stream().mapToInt(container.getStarts()::get).toArray();
				int[] entityLengths = entities.stream().mapToInt(container.getLengths()::get).toArray();

				// First entity of Bucket starts at 0, the following are appended.
				int[] entityStarts = Arrays.copyOf(entityLengths, entityLengths.length);
				entityStarts[0] = 0;
				for (int index = 1; index < entityLengths.length; index++) {
					entityStarts[index] = entityStarts[index - 1] + entityLengths[index - 1];
				}

				// copy only the parts of the bucket we need
				final CType<?, ?>[] bucketStores =
						Arrays.stream(stores)
							  .map(store -> store.select(selectionStart, entityLengths))
							  .toArray(CType<?, ?>[]::new);


				final Bucket bucket = new Bucket(
						currentBucket,
						outImport.getId(),
						Arrays.stream(entityLengths).sum(),
						bucketStores,
						new Int2IntArrayMap(globalIds, entityStarts),
						new Int2IntArrayMap(globalIds, entityLengths),
						globalIds.length
				);
				sendBucket(new ImportBucket(bucket));
			}
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to load the file " + importFile, exception);
		}
	}

	private PreprocessedHeader readHeader(HCFile file) throws JsonParseException, IOException {
		try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
			PreprocessedHeader header = headerReader.readValue(in);


			Table tab = namespace.getStorage().getDataset().getTables().getOrFail(table);

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

			log.debug("sending");

			namespace.sendToAll(new UpdateDictionary(primaryDict));
		}
		return primaryMapping;
	}

	private Map<String, DictionaryMapping> importDictionaries(PreprocessedHeader header, Map<String, Dictionary> dicts)
			throws JSONException {
		final Map<String, DictionaryMapping> out = new HashMap<>();

		log.debug("sending secondary dictionaries");

		Table table = namespace.getStorage().getDataset().getTables().get(this.table);

		for (int colPos = 0; colPos < header.getColumns().length; colPos++) {
			Column tableCol = table.getColumns()[colPos];
			//if the column uses a shared dictionary we have to merge the existing dictionary into that

			if (tableCol.getType() != MajorTypeId.STRING) {
				continue;
			}

			// if the target column has a shared dictionary, we merge them and then update the merged dictionary.
			if (tableCol.getSharedDictionary() != null) {
				final DictionaryId sharedDictionaryId = new DictionaryId(namespace.getDataset().getId(), tableCol.getSharedDictionary());
				final Dictionary dictionary = dicts.get(tableCol.getName());

				final DictionaryMapping mapping = importSharedDictionary(dictionary, sharedDictionaryId);

				out.put(tableCol.getName(), mapping);
			}
			else if (dicts.containsKey(tableCol.getName())) {
				//store external infos into master and slaves
				final Dictionary dict = dicts.get(tableCol.getName());
				final DictionaryId dictionaryId = computeDefaultDictionaryId(header.getName(), tableCol);

				try {
					dict.setDataset(dictionaryId.getDataset());
					dict.setName(dictionaryId.getDictionary());

					namespace.getStorage().updateDictionary(dict);
					namespace.sendToAll(new UpdateDictionary(dict));
				}
				catch (Exception e) {
					throw new RuntimeException("Failed to store dictionary " + dict, e);
				}
			}
		}

		return out;
	}

	public void setDictionaryIds(PreprocessedHeader header, Table tableDescription, CType<?, ?>[] values) {
		for (int i = 0; i < tableDescription.getColumns().length; i++) {
			Column column = tableDescription.getColumns()[i];

			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			CType<?, ?> storeColumn = values[i];
			final StringType stringType = (StringType) storeColumn;

			// if no mapping provided, use incoming dict
			if (column.getSharedDictionary() != null) {
				stringType.setUnderlyingDictionary(computeSharedDictionaryId(column));
			}
			else {
				stringType.setUnderlyingDictionary(computeDefaultDictionaryId(header.getName(), column));

			}
		}
	}

	public void applyDictionaryMappings(Table tableDescription, Map<String, DictionaryMapping> mappings, CType<?, ?>[] values) {
		for (int i = 0; i < values.length; i++) {
			Column column = tableDescription.getColumns()[i];

			if (column.getType() != MajorTypeId.STRING) {
				continue;
			}

			CType<?, ?> storeColumn = values[i];
			final StringType stringType = (StringType) storeColumn;

			if (!mappings.containsKey(column.getName())) {
				continue;
			}

			// apply mapping
			final DictionaryMapping mapping = mappings.get(column.getName());

			// mapping is null, if this is the first dict to be loaded.
			if (mapping == null) {
				continue;
			}

			mapping.applyToStore(stringType, storeColumn.getLines());
		}
	}

	private Import createImport(PreprocessedHeader header, CType<?, ?>[] columns) {
		// todo what does this function do actually?
		Import imp = new Import(table);
		//TODO also store the dictionary ids in here then remove them on deletion of the import
		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());
		imp.setColumns(new ImportColumn[header.getColumns().length]);

		for (int i = 0; i < header.getColumns().length; i++) {
			PPColumn src = header.getColumns()[i];
			ImportColumn col = new ImportColumn();
			col.setName(src.getName());
			col.setType(columns[i].select(new int[0], new int[0])); // ie just the representation
			col.setParent(imp);
			col.setPosition(i);
			imp.getColumns()[i] = col;
		}
		return imp;
	}

	/**
	 * Group entities by their global bucket id.
	 */
	public Map<Integer, List<Integer>> groupByBucket(Set<Integer> entities, DictionaryMapping primaryMapping, int bucketSize) {
		return entities.stream()
					   .collect(Collectors.groupingBy(entity -> Entity.getBucket(primaryMapping.source2Target(entity), bucketSize)));

	}

	private void sendBucket(ImportBucket bucket) {
		int bucketNumber = bucket.getBucket().getBucket();

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

		responsibleWorker.send(bucket);
	}

	private DictionaryMapping importSharedDictionary(Dictionary incoming, DictionaryId targetDictionary) throws JSONException {

		log.info("merging into shared Dictionary[{}]", targetDictionary);

		Dictionary shared = namespace.getStorage().getDictionary(targetDictionary);
		DictionaryMapping mapping = null;

		// todo is reusing worth the code weirdness?
		// we can reuse the incoming dict if there is no prior.
		if (shared == null) {
			shared = incoming;
			shared.setDataset(targetDictionary.getDataset());
			shared.setName(targetDictionary.getDictionary());
		}
		else {
			mapping = DictionaryMapping.create(incoming, Dictionary.copyUncompressed(shared));
			mapping.getTargetDictionary().setName(targetDictionary.getDictionary());
			mapping.getTargetDictionary().setDataset(targetDictionary.getDataset());
		}

		namespace.getStorage().updateDictionary(shared);
		namespace.sendToAll(new UpdateDictionary(shared));

		return mapping;
	}

	public DictionaryId computeDefaultDictionaryId(String importName, Column column) {
		return new DictionaryId(namespace.getDataset().getId(), String.format("%s#%s", importName, column.getId().toString()));
	}

	private DictionaryId computeSharedDictionaryId(Column column) {
		return new DictionaryId(namespace.getDataset().getId(), column.getSharedDictionary());
	}

	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importFile;
	}

}
