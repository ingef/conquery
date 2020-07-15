package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerStorageImpl extends NamespacedStorageImpl implements WorkerStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> blocks;
	private IdentifiableStore<CBlock> cBlocks;
	@Getter
	private BucketManager bucketManager;
	
	public WorkerStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
	}
	
	@Override
	public void setBucketManager(BucketManager bucketManager) {
		this.bucketManager = bucketManager;
	}

	@Override
	protected List<ListenableFuture<KeyIncludingStore<?, ?>>> createStores(ListeningExecutorService pool) throws ExecutionException, InterruptedException {

		worker = StoreInfo.WORKER.singleton(getEnvironment(), getValidator());
		blocks = StoreInfo.BUCKETS.identifiable(getEnvironment(), getValidator(), getCentralRegistry());
		cBlocks = StoreInfo.C_BLOCKS.identifiable(getEnvironment(), getValidator(), getCentralRegistry());


		// Load all base data first, then load worker specific data.
		final List<ListenableFuture<KeyIncludingStore<?, ?>>> stores = super.createStores(pool);
		Futures.allAsList(stores).get();

		return ImmutableList.<ListenableFuture<KeyIncludingStore<?, ?>>>builder()
					   .addAll(stores)
					   .add(
							   pool.submit(blocks::loadData, blocks),
							   pool.submit(worker::loadData, worker),
							   pool.submit(cBlocks::loadData, cBlocks)
					   )
					   .build();
	}

	@Override
	public void addCBlock(CBlock cBlock) throws JSONException {
		cBlocks.add(cBlock);
	}

	@Override
	public CBlock getCBlock(CBlockId id) {
		return cBlocks.get(id);
	}

	@Override
	public void updateCBlock(CBlock cBlock) throws JSONException {
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
	public void addBucket(Bucket bucket) throws JSONException {
		blocks.add(bucket);
		if(this.getBucketManager() != null) {
			this.getBucketManager().addBucket(bucket);
		}
	}

	@Override
	public Bucket getBucket(BucketId id) {
		return blocks.get(id);
	}
	
	@Override
	public void removeBucket(BucketId id) {
		blocks.remove(id);
		if(this.getBucketManager() != null) {
			this.getBucketManager().removeBucket(id);
		}
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
	public void setWorker(WorkerInformation worker) throws JSONException {
		this.worker.add(worker);
	}

	@Override
	public void updateWorker(WorkerInformation worker) throws JSONException {
		this.worker.update(worker);
	}
	
	//block manager overrides
	@Override
	public void updateConcept(Concept<?> concept) throws JSONException {
		concepts.update(concept);
		if(bucketManager != null) {
			bucketManager.removeConcept(concept.getId());
			bucketManager.addConcept(concept);
		}
	}

	@Override
	public void removeConcept(ConceptId id) {
		concepts.remove(id);
		if(bucketManager != null) {
			bucketManager.removeConcept(id);
		}
	}

	@Override
	public void removeImport(ImportId id){
		imports.remove(id);

		if (bucketManager != null){
			bucketManager.removeImport(id);
		}
	}
}
