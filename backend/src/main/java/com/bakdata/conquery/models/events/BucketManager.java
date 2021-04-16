package com.bakdata.conquery.models.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.IdMutex.Locked;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.jobs.CalculateCBlocksJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Worker;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @implNote This class is only used per {@link Worker}. And NOT in the ManagerNode.
 */
@Slf4j
@RequiredArgsConstructor
public class BucketManager {

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;
	//Backreference
	private final Worker worker;
	@Getter
	private final Int2ObjectMap<Entity> entities;


	/**
	 * The final Map is the way the APIs expect the data to be delivered.
	 * <p>
	 * Connector -> Bucket -> [BucketId -> CBlock]
	 */
	private final Map<Connector, Int2ObjectMap<Map<Bucket, CBlock>>> connectorToCblocks;

	/**
	 * Table -> BucketN -> [Buckets]
	 */
	private final Map<Table, Int2ObjectMap<List<Bucket>>> tableToBuckets;

	@Getter
	private final int entityBucketSize;

	public static BucketManager create(Worker worker, WorkerStorage storage, int entityBucketSize) {
		Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();
		Map<Connector, Int2ObjectMap<Map<Bucket, CBlock>>> connectorCBlocks = new HashMap<>();
		Map<Table, Int2ObjectMap<List<Bucket>>> tableBuckets = new HashMap<>();

		IntArraySet assignedBucketNumbers = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets that map to: {}", assignedBucketNumbers);

		for (Bucket bucket : storage.getAllBuckets()) {
			if (!assignedBucketNumbers.contains(bucket.getBucket())) {
				log.warn("Found Bucket[{}] in Storage that does not belong to this Worker according to the Worker information.", bucket.getId());
			}
			registerBucket(bucket, entities, tableBuckets);
		}

		for (CBlock cBlock : storage.getAllCBlocks()) {
			registerCBlock(cBlock, connectorCBlocks);
		}

		return new BucketManager(worker.getJobManager(), storage, worker, entities, connectorCBlocks, tableBuckets, entityBucketSize);
	}

	/**
	 * register entities, and create query specific indices for bucket
	 */
	private static void registerBucket(Bucket bucket, Int2ObjectMap<Entity> entities, Map<Table, Int2ObjectMap<List<Bucket>>> tableBuckets) {
		for (int entity : bucket.entities()) {
			entities.computeIfAbsent(entity, Entity::new);
		}

		tableBuckets
				.computeIfAbsent(bucket.getTable(), id -> new Int2ObjectAVLTreeMap<>())
				.computeIfAbsent(bucket.getBucket(), n -> new ArrayList<>())
				.add(bucket);
	}

	/**
	 * Assert validity of operation, and create index for CBlocks.
	 */
	private static void registerCBlock(CBlock cBlock, Map<Connector, Int2ObjectMap<Map<Bucket, CBlock>>> connectorCBlocks) {
		connectorCBlocks.computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
						.computeIfAbsent(cBlock.getBucket().getBucket(), bucketId -> new HashMap<>(3))
						.put(cBlock.getBucket(), cBlock);
	}

	@SneakyThrows
	public void fullUpdate() {
		for (Concept<?> c : storage.getAllConcepts()) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {

					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con);

					for (Bucket bucket : storage.getAllBuckets()) {

						CBlockId cBlockId = new CBlockId(bucket.getId(), con.getId());

						if (hasCBlock(cBlockId)) {
							log.trace("Skip calculation of CBlock[{}], because it was loaded from the storage.", cBlockId);
							continue;
						}

						log.warn("CBlock[{}] missing in Storage. Queuing recalculation", cBlockId);
						job.addCBlock(bucket, cBlockId);
					}


					if (!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		registerCBlock(cBlock, connectorToCblocks);
	}

	public void addBucket(Bucket bucket) {
		storage.addBucket(bucket);
		registerBucket(bucket, entities, tableToBuckets);

		for (Concept<?> concept : storage.getAllConcepts()) {
			for (Connector connector : concept.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(connector.getId())) {
					CBlockId cBlockId = new CBlockId(bucket.getId(), connector.getId());

					if (!connector.getTable().equals(bucket.getTable())) {
						continue;
					}
					if (hasCBlock(cBlockId)) {
						continue;
					}

					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, connector);

					job.addCBlock(bucket, cBlockId);
					jobManager.addSlowJob(job);
				}
			}
		}
	}

	public void addConcept(Concept<?> concept) {
		storage.updateConcept(concept);

		for (Connector connector : concept.getConnectors()) {
			try (Locked lock = cBlockLocks.acquire(connector.getId())) {
				Table table = connector.getTable();
				CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, connector);

				for (Bucket bucket : storage.getAllBuckets()) {
					if (!bucket.getTable().equals(table)) {
						continue;
					}

					final CBlockId cBlockId = new CBlockId(bucket.getId(), connector.getId());

					if (!hasCBlock(cBlockId)) {
						continue;
					}

					job.addCBlock(bucket, cBlockId);
				}

				if (!job.isEmpty()) {
					jobManager.addSlowJob(job);
				}
				else {
					log.debug("CBlocksJob[{}] was empty", job.getLabel());
				}
			}
		}
	}

	private void deregisterBucket(Bucket bucket) {
		for (int entityId : bucket.entities()) {
			final Entity entity = entities.get(entityId);

			if (entity == null) {
				continue;
			}

			if (isEntityEmpty(entity)) {
				entities.remove(entityId);
			}
		}

		tableToBuckets.getOrDefault(bucket.getTable(), Int2ObjectMaps.emptyMap())
					  .getOrDefault(bucket.getBucket(), Collections.emptyList())
					  .removeIf(bkt -> bkt.getId().equals(bucket.getId()));

	}

	public void removeBucket(Bucket bucket) {
		storage.getAllCBlocks()
			   .stream()
			   .filter(cblock -> cblock.getBucket().equals(bucket))
			   .forEach(this::removeCBlock);

		deregisterBucket(bucket);

		storage.removeBucket(bucket.getId());
	}

	public void removeConcept(Concept<?> concept) {

		storage.getAllCBlocks().stream()
			   .filter(cBlock -> cBlock.getConnector().getConcept().equals(concept))
			   .forEach(this::removeCBlock);

		storage.removeConcept(concept.getId());
	}

	private void removeCBlock(CBlock cBlock) {

		deregisterCBlock(cBlock);

		storage.removeCBlock(cBlock.getId());
	}

	private void deregisterCBlock(CBlock cBlock) {
		Bucket bucket = cBlock.getBucket();

		connectorToCblocks.getOrDefault(cBlock.getConnector(), Int2ObjectMaps.emptyMap())
						  .getOrDefault(cBlock.getBucket().getBucket(), Collections.emptyMap())
						  .values()
						  .removeIf(cBlock::equals);

		for (int entityId : bucket.entities()) {
			final Entity entity = entities.get(entityId);

			if (entity == null) {
				continue;
			}

			//TODO Verify that this is enough.
			if (isEntityEmpty(entity)) {
				entities.remove(entityId);
			}
		}
	}

	/**
	 * Test if there is any known associated data to the Entity in the {@link BucketManager}
	 *
	 * @param entity
	 */
	public boolean isEntityEmpty(Entity entity) {
		return !hasBucket(Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize()));
	}

	private boolean hasBucket(int id) {
		return storage.getAllBuckets().stream().map(Bucket::getBucket).anyMatch(bucket -> bucket == id);
	}

	/**
	 * Remove all buckets comprising the import. Which will in-turn remove all CBLocks.
	 */
	public void removeImport(Import imp) {
		storage.getAllBuckets()
			   .stream()
			   .filter(bucket -> bucket.getImp().equals(imp))
			   .forEach(this::removeBucket);


		for (Concept<?> concept : storage.getAllConcepts()) {
			if (!(concept instanceof TreeConcept)) {
				continue;
			}

			((TreeConcept) concept).removeImportCache(imp);
		}
		storage.removeImport(imp.getId());
	}

	public boolean hasCBlock(CBlockId id) {
		return storage.getCBlock(id) != null;
	}

	public List<Bucket> getEntityBucketsForTable(Entity entity, Table table) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		return tableToBuckets.getOrDefault(table, Int2ObjectMaps.emptyMap())
							 .getOrDefault(bucketId, Collections.emptyList());
	}

	public Map<Bucket, CBlock> getEntityCBlocksForConnector(Entity entity, Connector connector) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		return connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap())
								 .getOrDefault(bucketId, Collections.emptyMap());
	}

	public boolean hasEntityCBlocksForConnector(Entity entity, Connector connector) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());
		return connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap())
								 .containsKey(bucketId);
	}
}
