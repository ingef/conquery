package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.BlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerStorageImpl extends NamespacedStorageImpl implements WorkerStorage {

	@Getter
	private BlockManager blockManager;
	private final SingletonStore<WorkerInformation> worker;
	private final IdentifiableStore<Block> blocks;
	private final IdentifiableStore<CBlock> cBlocks;
	
	public WorkerStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
		this.worker = StoreInfo.WORKER.singleton(this);
		this.imports = new IdentifiableStore<Import>(centralRegistry, StoreInfo.IMPORTS.cached(this)) {
			@Override
			protected void addToRegistry(CentralRegistry centralRegistry, Import imp) throws ConfigurationException, JSONException {
				imp.loadExternalInfos(WorkerStorageImpl.this);
			}
		};
		this.blocks = StoreInfo.BLOCKS.identifiable(this);
		this.cBlocks = StoreInfo.C_BLOCKS.identifiable(this);
	}

	@Override
	public void stopStores() throws IOException {
		blocks.close();
		cBlocks.close();
		log.info("Stopped slave storage");
	}
	
	@Override
	public void setBlockManager(BlockManager blockManager) {
		this.blockManager = blockManager;
		if(this.getWorker()!=null) {
			blockManager.init(this.getWorker());
		}
	}

	@Override public Dictionary getPrimaryDictionary() {
		return null;
	}

	@Override
	public void updateConcept(Concept<?> concept) throws JSONException {
		concepts.update(concept);
		if(blockManager!=null) {
			blockManager.removeConcept(concept.getId());
			blockManager.addConcept(concept);
		}
	}

	@Override
	public void removeConcept(ConceptId id) {
		concepts.remove(id);
		if(blockManager!=null) {
			blockManager.removeConcept(id);
		}
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
	public void addBlocks(List<Block> newBlocks) throws JSONException {
		for(Block block:newBlocks) {
			blocks.add(block);
		}
		if(blockManager!=null) {
			blockManager.addBlocks(newBlocks);
		}
	}

	@Override
	public Block getBlock(BlockId id) {
		return blocks.get(id);
	}
	
	@Override
	public void removeBlock(BlockId id) {
		blocks.remove(id);
		if(blockManager!=null) {
			blockManager.removeBlock(id);
		}
	}
	
	@Override
	public Collection<Block> getAllBlocks() {
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
}
