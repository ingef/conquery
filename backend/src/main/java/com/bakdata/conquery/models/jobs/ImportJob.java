package com.bakdata.conquery.models.jobs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PPColumn;
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
import com.bakdata.conquery.util.RangeUtil;
import com.bakdata.conquery.util.io.Cloner;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.primitives.Ints;
import com.jakewharton.byteunits.BinaryByteUnit;
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
		this.progressReporter.setMax(16);
		try (HCFile file = new HCFile(importFile, false)) {

			if (log.isInfoEnabled()) {
				log.info(
						"Reading HCFile {}:\n\theader size: {}\n\tcontent size: {}",
						importFile,
						BinaryByteUnit.format(file.getHeaderSize()),
						BinaryByteUnit.format(file.getContentSize())
				);
			}
			PreprocessedHeader header = readHeader(file);

			//see #161  match to table check if it exists and columns are of the right type

			//check that all workers are connected
			namespace.checkConnections();

			//update primary dictionary
			boolean mappingRequired = createMappings(header);
			this.progressReporter.report(1);
			DictionaryMapping primaryMapping = header.getPrimaryColumn().getValueMapping();

			//partition the new IDs between the slaves
			log.debug("\tpartition new IDs");

			// Allocate a responsibility for all yet unassigned buckets.
			synchronized (namespace) {
				for (int bucket : primaryMapping.getUsedBuckets()) {
					if (namespace.getResponsibleWorkerForBucket(bucket) != null) {
						continue;
					}

					namespace.addResponsibility(bucket);
				}
			}

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new UpdateWorkerBucket(w));
			}

			synchronized (namespace) {
			}

			//update the allIdsTable
			log.info("\tupdating id information");
			Import allIdsImp = new Import();
			allIdsImp.setName(new ImportId(table, header.getName()).toString());
			allIdsImp.setTable(new TableId(namespace.getStorage().getDataset().getId(), ConqueryConstants.ALL_IDS_TABLE));
			allIdsImp.setNumberOfEntries(primaryMapping.getNumberOfNewIds());
			allIdsImp.setColumns(new ImportColumn[0]);
			allIdsImp.getBlockFactory(); //so that classes are created before storing/sending
			namespace.getStorage().updateImport(allIdsImp);
			namespace.sendToAll(new AddImport(allIdsImp));
			this.progressReporter.report(1);


			//create data import and store/send it
			log.info("\tupdating import information");
			//if mapping is not required we can also use the old infos here
			Import outImport = createImport(header, !mappingRequired);
			Import inImport = createImport(header, true);
			inImport.setSuffix(inImport.getSuffix()+"_old");
			namespace.getStorage().updateImport(outImport);
			namespace.sendToAll(new AddImport(outImport));

			this.progressReporter.report(1);
			int bucketSize = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();

			//import the new ids into the all ids table
			if (primaryMapping.getNewIds() != null) {
				BlockFactory factory = allIdsImp.getBlockFactory();
				Int2ObjectMap<ImportBucket> allIdsBuckets = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
				Int2ObjectMap<List<byte[]>> allIdsBytes = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
				try (Output buffer = new Output(2048)) {
					ProgressReporter child = this.progressReporter.subJob(5);
					child.setMax(primaryMapping.getNumberOfNewIds());

					for (int entityId : RangeUtil.iterate(primaryMapping.getNewIds())) {
						buffer.reset();
						Bucket bucket = factory.create(allIdsImp, Collections.singletonList(new Object[0]));
						bucket.writeContent(buffer);

						//copy content into ImportBucket
						int bucketNumber = Entity.getBucket(entityId, bucketSize);
						
						ImportBucket impBucket = allIdsBuckets
							.computeIfAbsent(bucketNumber, b->new ImportBucket(new BucketId(allIdsImp.getId(), b)));
						
						impBucket.getIncludedEntities().add(entityId);
						
						allIdsBytes
							.computeIfAbsent(bucketNumber, i->new ArrayList<>())
							.add(buffer.toBytes());
						
						child.report(1);
					}
				}
				sendBuckets(primaryMapping, allIdsBuckets, allIdsBytes);
			}
			
			//import the actual data
			log.info("\timporting");
			Int2ObjectMap<ImportBucket> buckets = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
			Int2ObjectMap<List<byte[]>> bytes = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
			ProgressReporter child = this.progressReporter.subJob(5);
			child.setMax(header.getGroups() + 1);
			InputStream inputStream = file.readContent();
			try (Input in = new Input(inputStream)) {
				for (long group = 0; group < header.getGroups(); group++) {
					int entityId = primaryMapping.source2Target(in.readInt(true));
					int size = in.readInt(true);
					int bucketNumber = Entity.getBucket(entityId, bucketSize);
					ImportBucket bucket = buckets
						.computeIfAbsent(bucketNumber, b->new ImportBucket(new BucketId(outImport.getId(), b)));
					
					byte[] data = in.readBytes(size);
					if(mappingRequired) {
						try(InputStream bounded = new ByteArrayInputStream(data);
							ByteArrayOutputStream out = new ByteArrayOutputStream(data.length+16)) {
							
							Bucket value = inImport.getBlockFactory().readSingleValue(bucketNumber, inImport, bounded);
							Bucket result = outImport.getBlockFactory().adaptValuesFrom(bucketNumber, outImport, value, header);
							try(Output sOut = new Output(out)) {
								result.writeContent(sOut);
							}
							data = out.toByteArray();
						}
					}
					
						
					bucket.getIncludedEntities().add(entityId);
					
					bytes
						.computeIfAbsent(bucketNumber, i->new ArrayList<>())
						.add(data);
					
					child.report(1);
				}
			}
			sendBuckets(primaryMapping, buckets, bytes);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load the file " + importFile, e);
		}
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
				useOldType?
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

	private void sendBuckets(DictionaryMapping primaryMapping, Int2ObjectMap<ImportBucket> buckets, Int2ObjectMap<List<byte[]>> bytes) {
		for(int bucketNumber : primaryMapping.getUsedBuckets()) {
			ImportBucket bucket = buckets.get(bucketNumber);
			//a bucket could be empty since the used buckets coming from the
			//dictionary could contain ids that have no events (see filter)
			if(bucket != null) {
				List<byte[]> buffers = bytes.get(bucketNumber);
				bucket.setBytes(buffers.toArray(new byte[0][]));
				
				WorkerInformation responsibleWorker = namespace.getResponsibleWorkerForBucket(bucketNumber);
				if (responsibleWorker == null) {
					throw new IllegalStateException("No responsible worker for bucket " + bucketNumber);
				}
				try {
					responsibleWorker.getConnectedSlave().waitForFreeJobqueue();
				} catch (InterruptedException e) {
					log.error("Interrupted while waiting for worker " + responsibleWorker + " to have free space in queue", e);
				}
				responsibleWorker.send(bucket);
			}
		}
	}

	private boolean createMappings(PreprocessedHeader header) throws JSONException {
		log.debug("\tupdating primary dictionary");
		Dictionary entities = ((StringTypeEncoded)header.getPrimaryColumn().getType()).getSubType().getDictionary();
		this.progressReporter.report(1);
		log.debug("\tcompute dictionary");
		Dictionary oldPrimaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
		Dictionary primaryDict = Dictionary.copyUncompressed(oldPrimaryDict);
		log.debug("\tmap values");
		DictionaryMapping primaryMapping = DictionaryMapping.create(entities, primaryDict);
		
		//if no new ids we shouldn't recompress and store
		if(primaryMapping.getNewIds() == null) {
			log.debug("\t\tno new ids");
			primaryDict = oldPrimaryDict;
			this.progressReporter.report(2);
		}
		//but if there are new ids we have to
		else {
			log.debug("\t\tnew ids {}", primaryMapping.getNewIds());
			log.debug("\t\texample of new id: {}", primaryDict.getElement(primaryMapping.getNewIds().getMin()));
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
		for(int colPos = 0; colPos<header.getColumns().length; colPos++) {
			PPColumn col = header.getColumns()[colPos];
			Column tableCol = table.getColumns()[colPos];
			//if the column uses a shared dictionary we have to merge the existing dictionary into that
			if(tableCol.getType() == MajorTypeId.STRING && tableCol.getSharedDictionary() != null) {
				mappingRequired |= createSharedDictionary(col, tableCol);
			}

			//store external infos into master and slaves
			col.getType().storeExternalInfos(namespace.getStorage(),
				(Consumer<Dictionary>)(dict -> {
					try {
						namespace.getStorage().updateDictionary(dict);
						namespace.sendToAll(new UpdateDictionary(dict));
					} catch(Exception e) {
						throw new RuntimeException("Failed to store dictionary "+dict, e);
					}
				})
			);
		}
		header.getPrimaryColumn().setValueMapping(primaryMapping);
		return mappingRequired;
	}

	private boolean createSharedDictionary(PPColumn col, Column tableCol) throws JSONException {
		AStringType<?> oldType = (AStringType<?>) col.getType();
		Dictionary source = oldType.getUnderlyingDictionary();
		//could be null if the strin column has no (or too few) values
		if(source == null) {
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

	private PreprocessedHeader readHeader(HCFile file) throws JsonParseException, IOException {
		try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
			PreprocessedHeader header = headerReader.readValue(in);

			log.info("Importing {} into {}", header.getName(), table);
			Table tab = namespace.getStorage().getDataset().getTables().getOrFail(table);

			if (!header.matches(tab)) {
				throw new IllegalArgumentException("The given header " + header + " does not match the table structure of " + table);
			}

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

	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importFile;
	}

}
