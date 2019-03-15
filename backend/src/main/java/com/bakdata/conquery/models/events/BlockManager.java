package com.bakdata.conquery.models.events;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.IdMutex.Locked;
import com.bakdata.conquery.models.identifiable.ids.specific.BlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.jobs.CalculateCBlocksJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Worker;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;

public class BlockManager {

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;
	private final Worker worker;
	private final IdMap<ConceptId, Concept<?>> concepts = new IdMap<>();
	private final IdMap<BlockId, Block> blocks = new IdMap<>();
	private final IdMap<CBlockId, CBlock> cBlocks = new IdMap<>();
	@Getter
	private final Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();

	public BlockManager(JobManager jobManager, WorkerStorage storage, Worker worker) {
		this.jobManager = jobManager;
		this.storage = storage;
		this.worker = worker;
		this.concepts.addAll(storage.getAllConcepts());
		this.blocks.addAll(storage.getAllBlocks());
		this.cBlocks.addAll(storage.getAllCBlocks());
		
		jobManager.addSlowJob(new SimpleJob("Update Block Manager", this::fullUpdate));
	}
	
	private void fullUpdate() {
		for(Concept<?> c:concepts) {
			for(Connector con:c.getConnectors()) {
				try(Locked lock = cBlockLocks.acquire(con.getId())) {
					Table t = con.getTable();
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
					ConnectorId conName = con.getId();
					for(Import imp:t.findImports(storage)) {
						for(int bucket : worker.getInfo().getIncludedBuckets()) {
							for(int entity : Entity.iterateBucket(bucket)) {
								BlockId blockId = new BlockId(imp.getId(), entity);
								Optional<Block> block = blocks.getOptional(blockId);
								if(block.isPresent()) {
									CBlockId cBlockId = new CBlockId(blockId, conName);
									if(!cBlocks.getOptional(cBlockId).isPresent()) {
										job.addCBlock(imp, block.get(), cBlockId);
									}
								}
							}
						}
					}
					if(!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}
		
		for(Block block : blocks) {
			entities
				.computeIfAbsent(block.getEntity(), Entity::new)
				.addBlock(storage.getCentralRegistry().resolve(block.getImp().getTable()), block);
		}
		
		for(CBlock cBlock : cBlocks) {
			entities
				.computeIfAbsent(cBlock.getBlock().getEntity(), Entity::new)
				.addCBlock(
						storage.getCentralRegistry().resolve(cBlock.getConnector()),
						storage.getImport(cBlock.getBlock().getImp()),
						storage.getCentralRegistry().resolve(cBlock.getBlock().getImp().getTable()),
						blocks.getOrFail(cBlock.getBlock()),
						cBlock
				);
		}
	}
	
	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		cBlocks.add(cBlock);
		entities
			.computeIfAbsent(cBlock.getBlock().getEntity(), Entity::new)
			.addCBlock(
					storage.getCentralRegistry().resolve(cBlock.getConnector()),
					storage.getImport(cBlock.getBlock().getImp()),
					storage.getCentralRegistry().resolve(cBlock.getBlock().getImp().getTable()),
					blocks.getOrFail(cBlock.getBlock()),
					cBlock
			);
	}
	
	public void addBlocks(List<Block> newBlocks) {
		for(Block block:newBlocks) {
			blocks.add(block);
			entities
				.computeIfAbsent(block.getEntity(), Entity::new)
				.addBlock(storage.getCentralRegistry().resolve(block.getImp().getTable()), block);
		}
		
		for(Concept<?> c:concepts) {
			ConceptId conceptName = c.getId();
			for(Connector con:c.getConnectors()) {
				try(Locked lock = cBlockLocks.acquire(con.getId())) {
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, con.getTable());
					for(Block block:newBlocks) {
						Import imp = block.getImp();
						if(con.getTable().getId().equals(block.getImp().getTable())) {
							CBlockId cBlockId = new CBlockId(
								block.getId(),
								con.getId()
							);
							if(!cBlocks.getOptional(cBlockId).isPresent()) {
								job.addCBlock(imp, block, cBlockId);
							}
						}
					}
					if(!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}
	}
	
	public void addConcept(Concept<?> c) {
		concepts.add(c);
		ConceptId conceptName = c.getId();
		
		for(Connector con:c.getConnectors()) {
			try(Locked lock = cBlockLocks.acquire(con.getId())) {
				Table t = con.getTable();
				CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
				for(Import imp : t.findImports(storage)) {
					for(int bucket : worker.getInfo().getIncludedBuckets()) {
						for(int entity : Entity.iterateBucket(bucket)) {
							BlockId blockId = new BlockId(imp.getId(), entity);
							Optional<Block> block = blocks.getOptional(blockId);
							if(block.isPresent()) {
								CBlockId cBlockId = new CBlockId(blockId, con.getId());
								if(!cBlocks.getOptional(cBlockId).isPresent()) {
									job.addCBlock(imp, block.get(), cBlockId);
								}
							}
						}
					}
				}
				if(!job.isEmpty()) {
					jobManager.addSlowJob(job);
				}
			}
		}
	}

	public void removeBlock(BlockId blockId) {
		Block block = blocks.remove(blockId);
		if(block!=null) {
			entities
				.computeIfAbsent(block.getEntity(), Entity::new)
				.removeBlock(blockId);

			for(Concept<?> c:concepts) {
				ConceptId conceptName = c.getId();
				for(Connector con:c.getConnectors()) {
					try(Locked lock = cBlockLocks.acquire(con.getId())) {
						if(con.getTable().getId().equals(block.getImp().getTable())) {
							CBlockId cBlockId = new CBlockId(
								blockId,
								con.getId()
							);
							if(cBlocks.remove(cBlockId) != null) {
								storage.removeCBlock(cBlockId);
								entities
									.computeIfAbsent(block.getEntity(), Entity::new)
									.removeCBlock(con, block);
							}
						}
					}
				}
			}
		}
	}

	public void removeConcept(ConceptId conceptId) {
		Concept<?> c = concepts.remove(conceptId);

		if (c == null) {
			return;
		}

		for(Connector con:c.getConnectors()) {

			try(Locked lock = cBlockLocks.acquire(con.getId())) {

				Table t = con.getTable();

				for(Import imp : t.findImports(storage)) {

					for(int bucket : worker.getInfo().getIncludedBuckets()) {

						for(int entity : Entity.iterateBucket(bucket)) {

							BlockId blockId = new BlockId(imp.getId(), entity);
							Optional<Block> block = blocks.getOptional(blockId);

							if(block.isPresent()) {

								CBlockId cBlockId = new CBlockId(blockId, con.getId());

								if(cBlocks.remove(cBlockId) != null) {
									storage.removeCBlock(cBlockId);
									entities
										.computeIfAbsent(entity, Entity::new)
										.removeCBlock(con, block.get());
								}
							}
						}
					}
				}
			}
		}
	}

	public boolean hasCBlock(CBlockId id) {
		return cBlocks.getOptional(id).isPresent();
	}
}
