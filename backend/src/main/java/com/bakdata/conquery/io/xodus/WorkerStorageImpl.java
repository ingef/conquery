package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.functions.Collector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerStorageImpl extends NamespacedStorageImpl implements WorkerStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> blocks;
	private IdentifiableStore<CBlock> cBlocks;
	@Getter
	private BucketManager bucketManager;

	@JsonIgnore
	private final ListMultimap<TableId, ImportId> tableImports = ArrayListMultimap.create();

	public WorkerStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
	}
	
	@Override
	public void setBucketManager(BucketManager bucketManager) {
		this.bucketManager = bucketManager;
	}

	@Override
	protected void createStores(Collector<KeyIncludingStore<?, ?>> collector) {
		super.createStores(collector);
		worker = StoreInfo.WORKER.singleton(getConfig(), getEnvironment(), getValidator());
		blocks = StoreInfo.BUCKETS.identifiable(getConfig(), getEnvironment(), getValidator(), getCentralRegistry());
		cBlocks = StoreInfo.C_BLOCKS.identifiable(getConfig(), getEnvironment(), getValidator(), getCentralRegistry());
		
		collector
			.collect(worker)
			.collect(blocks)
			.collect(cBlocks);
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
	public void addImport(Import imp) throws JSONException {
		super.addImport(imp);

		registerTableImport(imp.getId());
	}

	@Override
	public void updateImport(Import imp) throws JSONException {
		super.updateImport(imp);
	}

	@Override
	public void removeImport(ImportId id){
		super.removeImport(id);

		unregisterTableImport(id);

		if (bucketManager != null){
			bucketManager.removeImport(id);
		}
	}

	public void registerTableImport(ImportId impId) {
		tableImports.put(impId.getTable(),impId);
	}

	public void unregisterTableImport(ImportId impId) {
		tableImports.remove(impId.getTable(), impId);
	}

	public Collection<ImportId> getTableImports(TableId tableId) {
		return this.tableImports.get(tableId);
	}

}
