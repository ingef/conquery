package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
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
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.specific.VarIntParser;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.io.Cloner;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.jakewharton.byteunits.BinaryByteUnit;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ImportJob extends Job {

	private final ObjectReader headerReader = Jackson.BINARY_MAPPER.readerFor(PreprocessedHeader.class);

	private final Namespace namespace;
	private final TableId table;
	private final File importFile;

	@Override
	public void execute() throws JSONException {
		this.progressReporter.setMax(7);

		try (HCFile file = new HCFile(importFile, false)) {

			if (log.isInfoEnabled()) {
				log.info("Reading HCFile {}: header size: {}  content size: {}", importFile, BinaryByteUnit.format(file.getHeaderSize()), BinaryByteUnit.format(file.getContentSize()));
			}

			PreprocessedHeader header = readHeader(file);

			//check that all workers are connected
			namespace.checkConnections();

			//update primary dictionary
			boolean mappingRequired = createMappings(header);

			this.progressReporter.report(1);

			DictionaryMapping primaryMapping = header.getPrimaryColumn().getValueMapping();

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
			Import outImport = createImport(header, !mappingRequired);
			Import inImport = createImport(header, true);

			inImport.setSuffix(inImport.getSuffix() + "_old");

			namespace.getStorage().updateImport(outImport);
			namespace.sendToAll(new AddImport(outImport));

			this.progressReporter.report(1);


			//import the actual data
			log.info("\timporting");

			int bucketSize = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();

			Int2ObjectMap<ImportBucket> buckets = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());

			try (InputStream in = new GZIPInputStream(file.readContent())) {
				final Preprocessed.DataContainer container = Jackson.BINARY_MAPPER.readerFor(Preprocessed.DataContainer.class).readValue(in);

				if(container.getStarts() == null){
					log.warn("Import was empty. Skipping.");
					return;
				}

				final ColumnStore<?>[] stores = container.getValues();

				Multimap<Integer, Integer> buckets2LocalEntities = LinkedHashMultimap.create();

				// collect entities into their buckets
				for (Integer entity : container.getStarts().keySet()) {
					int entityId = primaryMapping.source2Target(entity);
					int currentBucket = Entity.getBucket(entityId, bucketSize);
					buckets2LocalEntities.put(currentBucket, entity);
				}

				for (Map.Entry<Integer, Collection<Integer>> bucket2entities : buckets2LocalEntities.asMap().entrySet()) {

					int currentBucket = bucket2entities.getKey();
					final List<Integer> entities = new ArrayList<>(bucket2entities.getValue());

					int[] selStart = new int[entities.size()];
					int[] selEnd = new int[entities.size()];

					for (int index = 0; index < entities.size(); index++) {
						int localId = entities.get(index);
						selStart[index] = container.getStarts().get(localId);
						selEnd[index] = container.getEnds().get(localId);
					}

					final List<ColumnStore<?>> list = new ArrayList<>(stores.length);
					list.addAll(Arrays.asList(stores));

					// copy only the parts of the bucket we need
					list.replaceAll(store -> store.select(selStart, selEnd));

					int[] lengths = IntStream.range(0, selStart.length)
							.map(index -> selEnd[index] - selStart[index])
							.toArray();

					final Int2IntMap starts = new Int2IntAVLTreeMap();
					final Int2IntMap ends = new Int2IntAVLTreeMap();

					starts.put(primaryMapping.source2Target(entities.get(0)), 0);
					ends.put(primaryMapping.source2Target(entities.get(0)), lengths[0]);

					for (int index = 1; index < lengths.length; index++) {
						int localId = entities.get(index);
						int globalId = primaryMapping.source2Target(localId);

						final int previousEntity = primaryMapping.source2Target(entities.get(index - 1));

						starts.put(globalId, ends.get(previousEntity));
						ends.put(globalId, starts.get(globalId) + lengths[index]);
					}


					buckets.put(currentBucket, new ImportBucket(
							new BucketId(outImport.getId(), currentBucket),
							new Bucket(
									currentBucket,
									outImport.getId(),
									ends.values().stream().mapToInt(i -> i).max().orElse(0),
									list.toArray(new ColumnStore[0]),
									starts,
									ends,
									starts.size()
							)));
				}
			}

			sendBuckets(primaryMapping, buckets);
			getProgressReporter().done();
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to load the file " + importFile, e);
		}
	}

	private PreprocessedHeader readHeader(HCFile file) throws JsonParseException, IOException {
		try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
			PreprocessedHeader header = headerReader.readValue(in);

			log.info("Importing {} into {}", header.getName(), table);
			Table tab = namespace.getStorage().getDataset().getTables().getOrFail(table);

			header.assertMatch(tab);

			log.debug("\tparsing dictionaries");
			header.getPrimaryColumn().getType().readHeader(in);
			for (PPColumn col : header.getColumns()) {
				col.getType().readHeader(in);
			}

			header.getPrimaryColumn().getType().init(namespace.getStorage().getDataset().getId());
			for (PPColumn col : header.getColumns()) {
				col.getType().init(namespace.getStorage().getDataset().getId());
			}

			return header;
		}
	}

	private boolean createMappings(PreprocessedHeader header) throws JSONException {
		log.debug("\tupdating primary dictionary");
		Dictionary entities = ((StringTypeEncoded) header.getPrimaryColumn().getType()).getSubType().getDictionary();
		this.progressReporter.report(1);
		log.debug("\tcompute dictionary");
		Dictionary oldPrimaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
		Dictionary primaryDict = Dictionary.copyUncompressed(oldPrimaryDict);
		log.debug("\tmap values");
		DictionaryMapping primaryMapping = DictionaryMapping.create(entities, primaryDict);

		//if no new ids we shouldn't recompress and store
		if (primaryMapping.getNewIds() == null) {
			log.debug("\t\tno new ids");
			primaryDict = oldPrimaryDict;
			this.progressReporter.report(2);
		}
		//but if there are new ids we have to
		else {
			log.debug("\t\t {} new ids {}", primaryMapping.getNumberOfNewIds(), primaryMapping.getNewIds());
			log.debug("\t\texample of new id: {}", new String(primaryDict.getElement(primaryMapping.getNewIds().getMin())));
			log.debug("\t\tstoring");

			namespace.getStorage().updateDictionary(primaryDict);

			this.progressReporter.report(1);

			log.debug("\t\tsending");

			namespace.sendToAll(new UpdateDictionary(primaryDict));

			this.progressReporter.report(1);
		}

		boolean mappingRequired = false;

		log.debug("\tsending secondary dictionaries");

		Table table = namespace.getStorage().getDataset().getTables().get(this.table);

		for (int colPos = 0; colPos < header.getColumns().length; colPos++) {
			PPColumn col = header.getColumns()[colPos];
			Column tableCol = table.getColumns()[colPos];
			//if the column uses a shared dictionary we have to merge the existing dictionary into that
			if (tableCol.getType() == MajorTypeId.STRING && tableCol.getSharedDictionary() != null) {
				mappingRequired |= createSharedDictionary(col, tableCol);
			}

			//store external infos into master and slaves
			col.getType().storeExternalInfos(
					namespace.getStorage(),
					(Consumer<Dictionary>) (dict -> {
						try {
							namespace.getStorage().updateDictionary(dict);
							namespace.sendToAll(new UpdateDictionary(dict));
						}
						catch (Exception e) {
							throw new RuntimeException("Failed to store dictionary " + dict, e);
						}
					})
			);
		}
		header.getPrimaryColumn().setValueMapping(primaryMapping);
		return mappingRequired;
	}

	private Import createImport(PreprocessedHeader header, boolean useOldType) {
		Import imp = new Import();
		imp.setName(header.getName());
		imp.setTable(table);
		imp.setNumberOfEntries(header.getRows());
		imp.setSuffix(header.getSuffix());
		imp.setColumns(new ImportColumn[header.getColumns().length]);
		for (int i = 0; i < header.getColumns().length; i++) {
			PPColumn src = header.getColumns()[i];
			ImportColumn col = new ImportColumn();
			col.setName(src.getName());
			col.setType(
					useOldType ?
					Objects.requireNonNullElse(src.getOldType(), src.getType())
							   :
					src.getType()
			);
			col.setParent(imp);
			col.setPosition(i);
			imp.getColumns()[i] = col;
		}
		return imp;
	}

	private void sendBuckets(DictionaryMapping primaryMapping, Int2ObjectMap<ImportBucket> buckets) {
		for (int bucketNumber : primaryMapping.getUsedBuckets()) {
			ImportBucket bucket = buckets.get(bucketNumber);
			//a bucket could be empty since the used buckets coming from the
			//dictionary could contain ids that have no events (see filter)
			if (bucket == null) {
				continue;
			}


			WorkerInformation responsibleWorker = namespace.getResponsibleWorkerForBucket(bucketNumber);
			if (responsibleWorker == null) {
				throw new IllegalStateException("No responsible worker for bucket " + bucketNumber);
			}
			try {
				responsibleWorker.getConnectedSlave().waitForFreeJobqueue();
			}
			catch (InterruptedException e) {
				log.error("Interrupted while waiting for worker " + responsibleWorker + " to have free space in queue", e);
			}
			responsibleWorker.send(bucket);
		}
	}

	private boolean createSharedDictionary(PPColumn col, Column tableCol) throws JSONException {
		AStringType<?> oldType = (AStringType<?>) col.getType();
		Dictionary source = oldType.getUnderlyingDictionary();
		//could be null if the strin column has no (or too few) values
		if (source == null) {
			return false;
		}
		DictionaryId sharedId = new DictionaryId(namespace.getDataset().getId(), tableCol.getSharedDictionary());
		log.info("\t\tmerging {} into shared dictionary {}", col.getName(), sharedId);

		Dictionary shared = namespace.getStorage().computeDictionary(sharedId);
		DictionaryMapping mapping = DictionaryMapping.create(
				source,
				shared
		);

		AStringType<?> newType = Cloner.clone(oldType);
		//find the new number type to represent the ids
		int minTargetId = Ints.min(mapping.getSource2TargetMap());
		int maxTargetId = Ints.max(mapping.getSource2TargetMap());
		VarIntParser numberParser = new VarIntParser();
		numberParser.registerValue(minTargetId);
		numberParser.registerValue(maxTargetId);
		numberParser.setLines(oldType.getLines());
		numberParser.setNullLines(oldType.getNullLines());
		Decision<Integer, Number, VarIntType> decision = numberParser.findBestType();

		newType.adaptUnderlyingDictionary(shared, decision.getType());
		col.setOldType(oldType);
		col.setTransformer(decision.getTransformer());
		col.setValueMapping(mapping);
		col.setType(newType);
		return true;
	}

	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importFile;
	}

}
