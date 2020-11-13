package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.ColumnStore;
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

			//check that all workers are connected
			namespace.checkConnections();


			//import the actual data
			log.info("\timporting");
			final Preprocessed.DataContainer container;
			try (InputStream in = new GZIPInputStream(file.readContent())) {
				container = Jackson.BINARY_MAPPER.readerFor(Preprocessed.DataContainer.class).readValue(in);
			}

			if (container.getStarts() == null) {
				log.warn("Import was empty. Skipping.");
				return;
			}

			final CType<?, ?>[] stores = container.getValues();

			// todo don't think this does anything useful
			for (CType<?, ?> col : container.getValues()) {
				col.init(namespace.getStorage().getDataset().getId());
			}

			DictionaryMapping primaryMapping = importPrimaryDictionary(container.getDictionaries().get("primary_dictionary"));
			//update primary dictionary

			for (CType<?, ?> column : container.getValues()) {
				column.loadExternalInfos(dict -> container.getDictionaries().get(dict.getDictionary()));
			}

			importDictionaries(header, container.getValues());

			// Distribute the new IDs between the slaves
			log.debug("\tpartition new IDs");

			// Allocate a responsibility for all yet unassigned buckets.
			synchronized (namespace) {
				for (int bucket : primaryMapping.getUsedBuckets()) {
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
			log.info("\tupdating import information");
			//if mapping is not required we can also use the old infos here
			Import outImport = createImport(header, stores);


			namespace.getStorage().updateImport(outImport);
			namespace.sendToAll(new AddImport(outImport));


			// but first remap String values
			// remapStores(header.getColumns(), stores, header.getRows());

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
				final CType<?,?>[] bucketStores =
						Arrays.stream(stores)
							  .map(store -> store.select(selectionStart, entityLengths))
							  .toArray(CType<?,?>[]::new);


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

			log.info("Importing {} into {}", header.getName(), table);
			Table tab = namespace.getStorage().getDataset().getTables().getOrFail(table);

			header.assertMatch(tab);

			return header;
		}
	}

	private DictionaryMapping importPrimaryDictionary(Dictionary underlyingDictionary) {
		log.debug("\tupdating primary dictionary");

		log.debug("\tcompute dictionary");

		Dictionary oldPrimaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
		Dictionary primaryDict = Dictionary.copyUncompressed(oldPrimaryDict);

		log.debug("\tmap values");
		DictionaryMapping primaryMapping = DictionaryMapping.create(underlyingDictionary, primaryDict, bucketSize);

		//if no new ids we shouldn't recompress and store
		if (primaryMapping.getNewIds() == null) {
			log.debug("\t\tno new ids");
		}
		//but if there are new ids we have to
		else {
			log.debug("\t\t {} new ids {}", primaryMapping.getNumberOfNewIds(), primaryMapping.getNewIds());
			log.debug("\t\tstoring");

			namespace.getStorage().updateDictionary(primaryDict);

			log.debug("\t\tsending");

			namespace.sendToAll(new UpdateDictionary(primaryDict));
		}
		return primaryMapping;
	}

	private void importDictionaries(PreprocessedHeader header, CType<?, ?>[] columns) throws JSONException {
		log.debug("\tsending secondary dictionaries");

		Table table = namespace.getStorage().getDataset().getTables().get(this.table);

		for (int colPos = 0; colPos < header.getColumns().length; colPos++) {
			Column tableCol = table.getColumns()[colPos];
			//if the column uses a shared dictionary we have to merge the existing dictionary into that

			if (!(columns[colPos] instanceof StringType)) {
				continue;
			}

			final StringType type = (StringType) columns[colPos];

			// if the target column has a shared dictionary, we merge them and then update the merged dictionary.
			if (tableCol.getSharedDictionary() != null) {
				createSharedDictionary(
						type.getUnderlyingDictionary(),
						new DictionaryId(namespace.getDataset().getId(), tableCol.getSharedDictionary()),
						type
				);
			}
			else if (type.getUnderlyingDictionary() != null) {
				//store external infos into master and slaves
				final Dictionary dict = type.getUnderlyingDictionary();
				try {
					dict.setDataset(namespace.getDataset().getId());
					namespace.getStorage().updateDictionary(dict);
					namespace.sendToAll(new UpdateDictionary(dict));
				}
				catch (Exception e) {
					throw new RuntimeException("Failed to store dictionary " + dict, e);
				}
			}
		}

	}

	private Import createImport(PreprocessedHeader header, CType<?,?>[] columns) {
		// todo what does this function do actually?
		Import imp = new Import(table);

		imp.setName(header.getName());
		imp.setNumberOfEntries(header.getRows());
		imp.setColumns(new ImportColumn[header.getColumns().length]);

		for (int i = 0; i < header.getColumns().length; i++) {
			PPColumn src = header.getColumns()[i];
			ImportColumn col = new ImportColumn();
			col.setName(src.getName());
			col.setType(columns[i].select(new int[0], new int[0]));
			col.setParent(imp);
			col.setPosition(i);
			imp.getColumns()[i] = col;
		}
		return imp;
	}

	public void remapStores(PPColumn[] columns, ColumnStore<?>[] stores, long nRows) {
		for (int i = 0; i < stores.length; i++) {
			ColumnStore<?> store = stores[i];
			final PPColumn column = columns[i];

			if (!(store instanceof StringType)) {
				continue;
			}

			if (column.getValueMapping() == null) {
				continue;
			}

			column.getValueMapping().applyToStore(((ColumnStore<Integer>) store), nRows);
		}
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
		try {
			responsibleWorker.getConnectedShardNode().waitForFreeJobqueue();
		}
		catch (InterruptedException e) {
			log.error("Interrupted while waiting for worker[{}] to have free space in queue", responsibleWorker, e);
		}
		responsibleWorker.send(bucket);
	}

	private void createSharedDictionary(Dictionary incoming, DictionaryId targetDictionary, StringType stringType) throws JSONException {
		// todo this can trivially be simplified to remove the boolean return value, further simplifying this section.


		log.info("\t\tmerging into shared Dictionary[{}]", targetDictionary);

		Dictionary shared = namespace.getStorage().computeDictionary(targetDictionary);

		DictionaryMapping mapping = DictionaryMapping.create(incoming, shared, bucketSize);


		mapping.applyToStore(stringType, stringType.getLines());
		stringType.adaptUnderlyingDictionary(shared);

		namespace.getStorage().updateDictionary(shared);
		namespace.sendToAll(new UpdateDictionary(shared));
	}

	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importFile;
	}

}
