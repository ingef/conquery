package com.bakdata.conquery.models.jobs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.messages.namespaces.specific.ImportBucket;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.RangeUtil;
import com.bakdata.conquery.util.io.SmallOut;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.powerlibraries.io.In;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
			Dictionary entities = new MapDictionary();
			DirectDictionary direct = new DirectDictionary(entities);
			ids.forEach(direct::add);
			
			log.debug("\tcompute dictionary");
			Dictionary oldPrimaryDict = namespace.getStorage().computeDictionary(ConqueryConstants.getPrimaryDictionary(namespace.getStorage().getDataset()));
			Dictionary primaryDict = Dictionary.copyUncompressed(oldPrimaryDict);
			log.debug("\tmap values");
			DictionaryMapping primaryMapping = DictionaryMapping.create(entities, primaryDict, namespace);
			
			//if no new ids we shouldn't recompress and store
			if(primaryMapping.getNewIds() == null) {
				log.debug("\t\tno new ids");
				primaryDict = oldPrimaryDict;
			}
			//but if there are new ids we have to
			else {
				log.debug("\t\tnew ids {}", primaryMapping.getNewIds());
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
			allIdsImp.setNumberOfEntries(ids.size());
			allIdsImp.setColumns(new ImportColumn[0]);
			namespace.getStorage().updateImport(allIdsImp);
			namespace.sendToAll(new AddImport(allIdsImp));

			int bucketSize = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();

			//import the new ids into the all ids table
			if (primaryMapping.getNewIds() != null) {
				BlockFactory factory = allIdsImp.getBlockFactory();
				Int2ObjectMap<ImportBucket> allIdsBuckets = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
				Int2ObjectMap<List<byte[]>> allIdsBytes = new Int2ObjectOpenHashMap<>(primaryMapping.getUsedBuckets().size());
				try (SmallOut buffer = new SmallOut(2048)) {
					ProgressReporter child = this.progressReporter.subJob(5);
					child.setMax(primaryMapping.getNewIds().getMax() - primaryMapping.getNewIds().getMin() + 1);

					for (int entityId : RangeUtil.iterate(primaryMapping.getNewIds())) {
						buffer.reset();
						Block block = factory.createBlock(entityId, allIdsImp, Collections.singletonList(new Object[0]));
						block.writeContent(buffer);

						//copy content into ImportBucket
						int size = buffer.position();
						int bucket = Entity.getBucket(entityId, bucketSize);
						
						allIdsBuckets
							.computeIfAbsent(bucket, b->new ImportBucket(new BucketId(allIdsImp.getId(), b)))
							.getIncludedEntities()
							.add(entityId);
						
						allIdsBytes
							.computeIfAbsent(bucket, i->new ArrayList<>())
							.add(buffer.toBytes());
						
						child.report(1);
					}
				}
				sendBuckets(primaryMapping, allIdsBuckets, allIdsBytes);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load the file " + importFile, e);
		}
	}
	
	private void sendBuckets(DictionaryMapping primaryMapping, Int2ObjectMap<ImportBucket> buckets, Int2ObjectMap<List<byte[]>> bytes) {
		for(int bucketNumber : primaryMapping.getUsedBuckets()) {
			ImportBucket bucket = buckets.get(bucketNumber);
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

	@Override
	public String getLabel() {
		return "Importing ids from " + importFile;
	}

}
