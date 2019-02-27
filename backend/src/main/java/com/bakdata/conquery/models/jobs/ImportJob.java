package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBits;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.RangeUtil;
import com.bakdata.conquery.util.io.GroupingByteBuffer;
import com.bakdata.conquery.util.io.MultiByteBuffer;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ImportJob extends Job {

	private final ObjectReader headerReader = Jackson.BINARY_MAPPER.readerFor(PPHeader.class);

	private final Namespace namespace;
	private final TableId table;
	private final File importFile;

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws JSONException {
		this.progressReporter.setMax(16);
		try (HCFile file = new HCFile(importFile, false)) {

			if (log.isInfoEnabled()) {
				log.info(
						"Reading HCFile {}:\n\theader size: {}\n\tcontent size: {}",
						importFile,
						FileUtils.byteCountToDisplaySize(file.getHeaderSize()),
						FileUtils.byteCountToDisplaySize(file.getContentSize())
				);
			}
			PPHeader header;
			try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
				header = headerReader.readValue(in);

				log.info("Importing {} into {}", header.getName(), table);
				Table tab = namespace.getStorage().getDataset().getTables().getOrFail(table);
				if (!tab.matches(header)) {
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
			}


			//see #161  match to table check if it exists and columns are of the right type

			//check that all workers are connected
			namespace.checkConnections();

			//update primary dictionary
			log.debug("\tupdating primary dictionary");
			//see #168  this leads to a crash if the primary values are not full strings
			Dictionary entities = ((StringType) header.getPrimaryColumn().getType()).getDictionary();
			this.progressReporter.report(1);
			log.debug("\tcompute dictionary");
			Dictionary primaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
			primaryDict = Dictionary.copyUncompressed(primaryDict);
			log.debug("\tmap values");
			DictionaryMapping primaryMapping = DictionaryMapping.create(entities, primaryDict);
			primaryDict.compress();
			log.debug("\t\tstoring");
			namespace.getStorage().updateDictionary(primaryDict);
			this.progressReporter.report(1);
			log.debug("\t\tsending");
			namespace.sendToAll(new UpdateDictionary(primaryDict));
			this.progressReporter.report(1);
			log.debug("\tsending secondary dictionaries");
			for(PPColumn col:header.getColumns()) {
				col.getType().storeExternalInfos(namespace.getStorage(),
					(Consumer<Dictionary>)(dict -> {
						try {
							namespace.getStorage().addDictionary(dict);
							namespace.sendToAll(new UpdateDictionary(dict));
						} catch(Exception e) {
							throw new RuntimeException("Failed to store dictionary "+dict, e);
						}
					})
				);
			}
			this.progressReporter.report(1);

			//partition the new IDs between the slaves
			log.debug("\tpartition new IDs");
			IntSet newBuckets = new IntOpenHashSet();
			for (int newId : RangeUtil.iterate(primaryMapping.getNewIds())) {
				if (namespace.getResponsibleWorker(newId) == null) {
					newBuckets.add(Entity.getBucket(newId, namespace.getEntityBucketSize()));
				}
			}
			for (int bucket : newBuckets) {
				namespace.addResponsibility(bucket);
			}

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new UpdateWorkerBucket(w));
			}

			namespace.updateWorkerMap();
			//namespace.getStorage().updateupdateMeta(namespace);

			//update the allIdsTable
			log.info("\tupdating id information");
			Import allIdsImp = new Import();
			allIdsImp.setName(new ImportId(table, header.getName()).toString());
			allIdsImp.setTable(new TableId(namespace.getStorage().getDataset().getId(), ConqueryConstants.ALL_IDS_TABLE));
			allIdsImp.setNumberOfBlocks(header.getGroups());
			allIdsImp.setNumberOfEntries(header.getGroups());
			allIdsImp.setColumns(new ImportColumn[0]);
			namespace.getStorage().updateImport(allIdsImp);
			namespace.sendToAll(new AddImport(allIdsImp));
			this.progressReporter.report(1);


			//create data import and store/send it
			log.info("\tupdating import information");
			Import imp = new Import();
			imp.setName(header.getName());
			imp.setTable(table);
			imp.setNumberOfBlocks(header.getGroups());
			imp.setNumberOfEntries(header.getRows());
			imp.setColumns(new ImportColumn[header.getColumns().length]);
			for (int i = 0; i < header.getColumns().length; i++) {
				PPColumn src = header.getColumns()[i];
				ImportColumn col = new ImportColumn();
				col.setName(src.getName());
				col.setType(src.getType());
				col.setParent(imp);
				col.setPosition(i);
				imp.getColumns()[i] = col;
			}
			namespace.getStorage().updateImport(imp);
			namespace.sendToAll(new AddImport(imp));

			this.progressReporter.report(1);

			//import the new ids into the all ids table
			if (primaryMapping.getNewIds() != null) {
				BlockFactory factory = allIdsImp.getBlockFactory();
				final Map<WorkerInformation, ImportBits> allIdsBits = new ConcurrentHashMap<>();
				for (WorkerInformation wi : namespace.getWorkers()) {
					allIdsBits.put(wi, new ImportBits(allIdsImp.getName(), allIdsImp.getId(), allIdsImp.getTable()));
				}
				try (MultiByteBuffer<WorkerInformation> allIdsBuffer = new MultiByteBuffer<>(namespace.getWorkers(), (worker, bytes) -> {
					ImportBits ib = allIdsBits.put(worker, new ImportBits(allIdsImp.getName(), allIdsImp.getId(), allIdsImp.getTable()));
					ib.setBytes(bytes);
					try {
						worker.getConnectedSlave().waitForFreeJobqueue();
					} catch (InterruptedException e) {
						log.error("Interrupted while waiting for worker " + worker + " to have free space in queue", e);
					}
					worker.send(ib);
				});
					Output buffer = new Output(2048);
				) {
					ProgressReporter child = this.progressReporter.subJob(5);
					child.setMax(primaryMapping.getNewIds().getMax() - primaryMapping.getNewIds().getMin() + 1);

					for (int entityId : RangeUtil.iterate(primaryMapping.getNewIds())) {
						buffer.clear();
						Block block = factory.createBlock(entityId, allIdsImp, Collections.singletonList(new Object[0]));
						block.writeContent(buffer);


						//copy content into ImportBits
						int size = buffer.position();
						WorkerInformation responsibleWorker = namespace.getResponsibleWorker(entityId);
						if (responsibleWorker == null) {
							throw new IllegalStateException("No responsible worker for " + entityId);
						}

						GroupingByteBuffer responsibleAllIdsBuffer = allIdsBuffer.get(responsibleWorker);
						responsibleAllIdsBuffer.ensureCapacity(size);
						allIdsBits.get(responsibleWorker).addBits(new ImportBits.Bit(entityId, size));
						System.arraycopy(buffer.getBuffer(), 0, responsibleAllIdsBuffer.internalArray(), responsibleAllIdsBuffer.offset(), size);
						responsibleAllIdsBuffer.advance(size);

						child.report(1);
					}
				}
			}

			//import the actual data
			log.info("\timporting");
			final Map<WorkerInformation, ImportBits> bits = new ConcurrentHashMap<>();
			for (WorkerInformation wi : namespace.getWorkers()) {
				bits.put(wi, new ImportBits(imp.getName(), imp.getId(), table));
			}
			try (Input in = new Input(file.readContent());
				MultiByteBuffer<WorkerInformation> buffer = new MultiByteBuffer<>(bits.keySet(), (worker, bytes) -> {
					ImportBits ib = bits.put(worker, new ImportBits(imp.getName(), imp.getId(), table));
					ib.setBytes(bytes);
					try {
						worker.getConnectedSlave().waitForFreeJobqueue();
					} catch (InterruptedException e) {
						log.error("Interrupted while waiting for worker " + worker + " to have free space in queue", e);
					}
					worker.send(ib);
				});
			) {

				ProgressReporter child = this.progressReporter.subJob(5);
				child.setMax(header.getGroups() + 1);
				for (long group = 0; group < header.getGroups(); group++) {
					//copy content into ImportBits
					int entityId = primaryMapping.source2Target(in.readInt(true));
					int size = in.readInt(true);
					WorkerInformation responsibleWorker = namespace.getResponsibleWorker(entityId);
					if (responsibleWorker == null) {
						throw new IllegalStateException("No responsible worker for " + entityId);
					}
					GroupingByteBuffer responsibleBuffer = buffer.get(responsibleWorker);
					responsibleBuffer.ensureCapacity(size);
					bits.get(responsibleWorker).addBits(new ImportBits.Bit(entityId, size));
					in.readBytes(responsibleBuffer.internalArray(), responsibleBuffer.offset(), size);
					responsibleBuffer.advance(size);

					child.report(1);
				}
			}
		} catch (KryoException | IOException e) {
			throw new IllegalStateException("Failed to load the file " + importFile, e);
		}
	}

	@Override
	public String getLabel() {
		return "Importing into " + table + " from " + importFile;
	}

}
