package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBits;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.RangeUtil;
import com.bakdata.conquery.util.io.GroupingByteBuffer;
import com.bakdata.conquery.util.io.MultiByteBuffer;
import com.bakdata.conquery.util.io.SmallOut;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.powerlibraries.io.In;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ImportIdsJob extends Job {

	private final ObjectReader headerReader = Jackson.BINARY_MAPPER.readerFor(PPHeader.class);

	private final Namespace namespace;
	private final File importFile;

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws JSONException {
		try {
			List<String> ids = In.file(importFile).withUTF8().readLines();

			if (log.isInfoEnabled()) {
				log.info(
						"Reading IDs file {}:\n\tcontent size: {}",
						importFile,
						ids.size()
				);
			}
			//check that all workers are connected
			namespace.checkConnections();

			//update primary dictionary
			log.debug("\tupdating primary dictionary");
			Dictionary entities = new Dictionary();
			ids.forEach(entities::add);
			entities.compress();
			
			log.debug("\tcompute dictionary");
			Dictionary oldPrimaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
			Dictionary primaryDict = Dictionary.copyUncompressed(oldPrimaryDict);
			log.debug("\tmap values");
			DictionaryMapping primaryMapping = DictionaryMapping.create(entities, primaryDict);
			
			//if no new ids we shouldn't recompress and store
			if(primaryMapping.getNewIds() == null) {
				log.debug("\t\tno new ids");
				primaryDict = oldPrimaryDict;
			}
			//but if there are new ids we have to
			else {
				log.debug("\t\tnew ids {}, recompressing", primaryMapping.getNewIds());
				primaryDict.compress();
				log.debug("\t\texample of new id: {}", primaryDict.getElement(primaryMapping.getNewIds().getMin()));
				log.debug("\t\tstoring");
				namespace.getStorage().updateDictionary(primaryDict);
				log.debug("\t\tsending");
				namespace.sendToAll(new UpdateDictionary(primaryDict));
			}
			
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
			allIdsImp.setName(importFile.toString());
			allIdsImp.setTable(new TableId(namespace.getStorage().getDataset().getId(), ConqueryConstants.ALL_IDS_TABLE));
			allIdsImp.setNumberOfBlocks(ids.size());
			allIdsImp.setNumberOfEntries(ids.size());
			allIdsImp.setColumns(new ImportColumn[0]);
			namespace.getStorage().updateImport(allIdsImp);
			namespace.sendToAll(new AddImport(allIdsImp));


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
					SmallOut buffer = new SmallOut(2048);
				) {

					for (int entityId : RangeUtil.iterate(primaryMapping.getNewIds())) {
						buffer.reset();
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

					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load the file " + importFile, e);
		}
	}

	@Override
	public String getLabel() {
		return "Importing ids from " + importFile;
	}

}
