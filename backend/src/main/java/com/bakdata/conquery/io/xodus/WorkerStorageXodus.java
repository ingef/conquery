package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerStorageXodus extends NamespacedStorageXodus implements WorkerStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> blocks;
	private IdentifiableStore<CBlock> cBlocks;


	public static WorkerStorageXodus tryLoad(Validator validator, XodusStorageFactory config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getName(), t));
		env.close();

		if(!exists) {
			return null;
		}

		WorkerStorageXodus storage = new WorkerStorageXodus(validator, directory, config);
		storage.loadData();
		return storage;
	}
	
	public WorkerStorageXodus(Validator validator, File directory, XodusStorageFactory config) {
		super(validator, config, directory, false);
	}

	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		super.createStores(environmentToStores);

		worker = StoreInfo.WORKER.singleton(getConfig().createStore(environment,validator,StoreInfo.WORKER));

		blocks = StoreInfo.BUCKETS.<Bucket>identifiable(getConfig().createStore(environment,validator,StoreInfo.BUCKETS), getCentralRegistry())
						 .onAdd((Bucket bucket) -> {
							 bucket.loadDictionaries(this);
						 });

		cBlocks = StoreInfo.C_BLOCKS.identifiable(getConfig().createStore(environment,validator,StoreInfo.C_BLOCKS), getCentralRegistry());
		
		environmentToStores.putAll(environment, List.of(
			worker, 
			blocks, 
			cBlocks
			));
	}
	
	@Override
	@SneakyThrows(JSONException.class)
	public void addCBlock(CBlock cBlock) {
		log.debug("Adding CBlock[{}]", cBlock.getId());
		cBlocks.add(cBlock);
	}

	@Override
	public CBlock getCBlock(CBlockId id) {
		return cBlocks.get(id);
	}

	// TODO method is unused, delete it.
	@Override
	@SneakyThrows(JSONException.class)
	public void updateCBlock(CBlock cBlock) {
		cBlocks.update(cBlock);
	}

	@Override
	public void removeCBlock(CBlockId id) {
		log.debug("Removing CBlock[{}]", id);
		cBlocks.remove(id);
	}

	@Override
	public void addDictionary(Dictionary dict) {
		if (dict.getId().equals(ConqueryConstants.getPrimaryDictionary(getDataset()))) {
			throw new IllegalStateException("Workers may not receive the primary dictionary");
		}

		super.addDictionary(dict);
	}

	@Override
	public Collection<CBlock> getAllCBlocks() {
		return cBlocks.getAll();
	}
	
	@Override
	@SneakyThrows(JSONException.class)
	public void addBucket(Bucket bucket) {
		log.debug("Adding Bucket[{}]", bucket.getId());
		blocks.add(bucket);
	}

	@Override
	public Bucket getBucket(BucketId id) {
		return blocks.get(id);
	}
	
	@Override
	public void removeBucket(BucketId id) {
		log.debug("Removing Bucket[{}]", id);
		blocks.remove(id);
	}
	
	@Override
	public Collection<Bucket> getAllBuckets() {
		return blocks.getAll();
	}

	@Override
	public WorkerInformation getWorker() {
		return worker.get();
	}

	//TODO remove duplication
	@Override
	@SneakyThrows(JSONException.class)
	public void setWorker(WorkerInformation worker) {
		this.worker.add(worker);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateWorker(WorkerInformation worker) {
		this.worker.update(worker);
	}
	
	//block manager overrides
	@Override
	@SneakyThrows(JSONException.class)
	public void updateConcept(Concept<?> concept) {
		log.debug("Updating Concept[{}]", concept.getId());
		concepts.update(concept);
	}

	@Override
	public void removeConcept(ConceptId id) {
		log.debug("Removing Concept[{}]", id);
		concepts.remove(id);
	}

}
