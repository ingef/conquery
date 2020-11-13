package com.bakdata.conquery.models.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.IdMutex.Locked;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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
	 *
	 * Connector -> Bucket -> [BucketId -> CBlock]
	 */
	private final Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlock>>> connectorToCblocks;

	/**
	 * Table -> BucketN -> [Buckets]
	 */
	private final Map<TableId, Int2ObjectMap<List<Bucket>>> tableToBuckets;

	public static BucketManager create(Worker worker, WorkerStorage storage) {
		Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();
		Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlock>>> connectorCBlocks = new HashMap<>();
		Map<TableId, Int2ObjectMap<List<Bucket>>> tableBuckets = new HashMap<>();

		IntArraySet assignedBucketNumbers = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets that map to: {}", assignedBucketNumbers);

		for (Bucket bucket : storage.getAllBuckets()) {
			if(!assignedBucketNumbers.contains(bucket.getBucket())) {
				log.warn("Found Bucket[{}] in Storage that does not belong to this Worker according to the Worker information.", bucket.getId());
			}
			registerBucket(bucket, entities, storage, tableBuckets);
		}

		for (CBlock cBlock : storage.getAllCBlocks()) {
			registerCBlock(cBlock, storage, connectorCBlocks);
		}

		return new BucketManager(worker.getJobManager(), storage, worker, entities, connectorCBlocks, tableBuckets);
	}

	/**
	 * register entities, and create query specific indices for bucket
	 */
	private static void registerBucket(Bucket bucket, Int2ObjectMap<Entity> entities, WorkerStorage storage, Map<TableId, Int2ObjectMap<List<Bucket>>> tableBuckets) {
		for (int entity : bucket) {
			entities.computeIfAbsent(entity, createEntityFor(bucket, storage));
		}

		tableBuckets
				.computeIfAbsent(bucket.getImp().getTable(), id -> new Int2ObjectAVLTreeMap<>())
				.computeIfAbsent(bucket.getBucket(), n -> new ArrayList<>())
				.add(bucket);
	}

	/**
	 * Assert validity of operation, and create index for CBlocks.
	 */
	private static void registerCBlock(CBlock cBlock, WorkerStorage storage, Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlock>>> connectorCBlocks) {

		Bucket bucket = storage.getBucket(cBlock.getBucket());
		if (bucket == null) {
			throw new NoSuchElementException("Could not find an element called '" + cBlock.getBucket() + "'");
		}

		connectorCBlocks
				.computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
				.computeIfAbsent(bucket.getId().getBucket(), bucketId -> new HashMap<>(3))
				.put(cBlock.getBucket(), cBlock);
	}

	/**
	 * Logic for tracing the creation of new Entities.
	 */
	private static IntFunction<Entity> createEntityFor(Identifiable<?> idable, WorkerStorage storage) {

		return id -> {

			if (log.isDebugEnabled() && idable.getId() instanceof NamespacedId) {
				byte[] thename = storage.getDictionary(ConqueryConstants.getPrimaryDictionary(((NamespacedId) idable.getId()).getDataset())).getElement(id);

				log.trace("Creating new Entitiy[{}]=`{}` for Bucket[{}]", id, new String(thename), idable.getId());
			}

			return new Entity(id);
		};
	}

	@SneakyThrows
	public void fullUpdate() {
		for (Concept<?> c : storage.getAllConcepts()) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {

					Table t = con.getTable();
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
					ConnectorId conName = con.getId();

					for (Import imp : t.findImports(storage)) {
						for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {
							BucketId bucketId = new BucketId(imp.getId(), bucketNumber);
							Bucket bucket = storage.getBucket(bucketId);
							if (bucket == null) {
								continue;
							}

							CBlockId cBlockId = new CBlockId(bucketId, conName);

							if (storage.getCBlock(cBlockId) != null) {
								log.trace("Skip calculation of CBlock[{}], because it was loaded from the storage.", cBlockId);
								continue;
							}

							log.warn("CBlock[{}] missing in Storage. Queuing recalculation", cBlockId);
							job.addCBlock(imp, bucket, cBlockId);
						}
					}

					if (!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		registerCBlock(cBlock, storage, connectorToCblocks);
	}

	public void addBucket(Bucket bucket) {
		storage.addBucket(bucket);
		registerBucket(bucket, entities, storage, tableToBuckets);

		for (Concept<?> c : storage.getAllConcepts()) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, con.getTable());
					Import imp = bucket.getImp();
					if (con.getTable().getId().equals(bucket.getImp().getTable())) {
						CBlockId cBlockId = new CBlockId(bucket.getId(), con.getId());

						if (storage.getCBlock(cBlockId) == null) {
							job.addCBlock(imp, bucket, cBlockId);
						}
					}
					if (!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}
	}

	public void addConcept(Concept<?> c) {
		storage.updateConcept(c);

		for (Connector con : c.getConnectors()) {
			try (Locked lock = cBlockLocks.acquire(con.getId())) {
				Table t = con.getTable();
				CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
				for (Import imp : t.findImports(storage)) {
					for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

						BucketId bucketId = new BucketId(imp.getId(), bucketNumber);
						Bucket bucket = storage.getBucket(bucketId);

						if (storage.getBucket(bucketId) == null) {
							continue;
						}

						CBlockId cBlockId = new CBlockId(bucketId, con.getId());

						if (storage.getCBlock(cBlockId) != null) {
							continue;
						}

						job.addCBlock(imp, bucket, cBlockId);
					}
				}
				if (!job.isEmpty()) {
					jobManager.addSlowJob(job);
				}
			}
		}
	}

	private void deregisterBucket(Bucket bucket) {
		for (int entityId : bucket) {
			final Entity entity = entities.get(entityId);

			if (entity == null) {
				continue;
			}

			if (isEntityEmpty(entity)) {
				entities.remove(entityId);
			}
		}

		tableToBuckets.getOrDefault(bucket.getImp().getTable(), Int2ObjectMaps.emptyMap())
					  .getOrDefault(bucket.getBucket(), Collections.emptyList())
					  .removeIf(bkt -> bkt.getId().equals(bucket.getId()));

	}

	public void removeBucket(BucketId bucketId) {
		Bucket bucket = storage.getBucket(bucketId);
		if (bucket == null) {
			return;
		}

		for (Concept<?> concept : storage.getAllConcepts()) {
			for (Connector con : concept.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					removeCBlock(new CBlockId(bucketId, con.getId()));
				}
			}
		}

		deregisterBucket(bucket);

		storage.removeBucket(bucketId);
	}

	public void removeConcept(ConceptId conceptId) {
		Concept<?> c = storage.getConcept(conceptId);

		if (c == null) {
			return;
		}

		for (Connector con : c.getConnectors()) {
			for (Import imp : con.getTable().findImports(storage)) {
				for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

					BucketId bucketId = new BucketId(imp.getId(), bucketNumber);

					if (storage.getBucket(bucketId) == null) {
						log.trace("Skipping cleanup of CBlock of Concept[{}] for Bucket[{}], because it is not present", conceptId, bucketId);
						continue;
					}

					removeCBlock(new CBlockId(bucketId, con.getId()));
				}
			}
		}

		storage.removeConcept(conceptId);
	}

	private void removeCBlock(CBlockId cBlockId) {
		if (storage.getCBlock(cBlockId) == null) {
			log.trace("Cannot remove CBlock[{}], because it is not present for worker[{}]", cBlockId, worker);
			return;
		}

		deregisterCBlock(cBlockId);

		storage.removeCBlock(cBlockId);
	}

	private void deregisterCBlock(CBlockId cBlockId) {
		Bucket bucket = storage.getBucket(cBlockId.getBucket());
		if (bucket == null) {
			throw new NoSuchElementException("Could not find an element called '" + cBlockId.getBucket() + "'");
		}

		connectorToCblocks.getOrDefault(cBlockId.getConnector(), Int2ObjectMaps.emptyMap())
						  .getOrDefault(cBlockId.getBucket().getBucket(), Collections.emptyMap())
						  .values()
						  .removeIf(cblock -> cblock.getId().equals(cBlockId));

		for (int entityId : bucket) {
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
	public void removeImport(ImportId imp) {
		for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

			BucketId bucketId = new BucketId(imp, bucketNumber);

			if (storage.getBucket(bucketId) == null) {
				continue;
			}

			removeBucket(bucketId);
		}

		for (Concept<?> concept : storage.getAllConcepts()) {
			if (!(concept instanceof TreeConcept)) {
				continue;
			}

			((TreeConcept) concept).removeImportCache(imp);
		}
		storage.removeImport(imp);
	}

	public boolean hasCBlock(CBlockId id) {
		return storage.getCBlock(id) != null;
	}

	public boolean hasBucket(BucketId id) {
		return storage.getBucket(id) != null;
	}

	public Bucket getBucket(BucketId id) {
		return storage.getBucket(id);
	}

	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId tableId) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());
		return tableToBuckets.getOrDefault(tableId, Int2ObjectMaps.emptyMap())
							 .getOrDefault(bucketId, Collections.emptyList());
	}

	public Map<BucketId, CBlock> getEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		return connectorToCblocks.getOrDefault(connectorId, Int2ObjectMaps.emptyMap())
								 .getOrDefault(bucketId, Collections.emptyMap());
	}

	public boolean hasEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());
		return connectorToCblocks.getOrDefault(connectorId, Int2ObjectMaps.emptyMap())
								 .containsKey(bucketId);
	}
}
