package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import lombok.SneakyThrows;

public class WorkerStorageImpl extends NamespacedStorageImpl implements WorkerStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> blocks;
	private IdentifiableStore<CBlock> cBlocks;
	
	public WorkerStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory, false);
	}

	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		super.createStores(environmentToStores);

		worker = StoreInfo.WORKER.singleton(getConfig(), environment, getValidator());

		blocks = StoreInfo.BUCKETS.<Bucket>identifiable(getConfig(), environment, getValidator(), getCentralRegistry())
						 .onAdd((Bucket bucket) -> {
							 bucket.loadDictionaries(this);
						 });

		cBlocks = StoreInfo.C_BLOCKS.identifiable(getConfig(), environment, getValidator(), getCentralRegistry());
		
		environmentToStores.putAll(environment, List.of(
			worker, 
			blocks, 
			cBlocks
			));
	}
	
	@Override
	@SneakyThrows(JSONException.class)
	public void addCBlock(CBlock cBlock) {
		cBlocks.add(cBlock);
	}

	@Override
	public CBlock getCBlock(CBlockId id) {
		return cBlocks.get(id);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateCBlock(CBlock cBlock) {
		cBlocks.update(cBlock);
	}

	@Override
	public void removeCBlock(CBlockId id) {
		cBlocks.remove(id);
	}
	
	@Override
	public Collection<CBlock> getAllCBlocks() {
		return cBlocks.getAll();
	}
	
	@Override
	@SneakyThrows(JSONException.class)
	public void addBucket(Bucket bucket) {
		blocks.add(bucket);
	}

	@Override
	public Bucket getBucket(BucketId id) {
		return blocks.get(id);
	}
	
	@Override
	public void removeBucket(BucketId id) {
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
		concepts.update(concept);
	}

	@Override
	public void removeConcept(ConceptId id) {
		concepts.remove(id);
	}

}
