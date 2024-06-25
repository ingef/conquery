package com.bakdata.conquery.models.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.IdMutex.Locked;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.jobs.CalculateCBlocksJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.worker.Worker;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to compute simple caches used in QueryEngine.
 *
 * @implNote This class is only used per {@link Worker}. And NOT in the ManagerNode.
 */
@Slf4j
@RequiredArgsConstructor
public class BucketManager {

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;

	private final Worker worker;

	private final Object2IntMap<String> entity2Bucket;


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
		final Map<Connector, Int2ObjectMap<Map<Bucket, CBlock>>> connectorCBlocks = new HashMap<>();
		final Map<Table, Int2ObjectMap<List<Bucket>>> tableBuckets = new HashMap<>();
		final Object2IntMap<String> entity2Bucket = new Object2IntOpenHashMap<>();

		final IntArraySet assignedBucketNumbers = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets that map to: {}", assignedBucketNumbers);

		for (Bucket bucket : storage.getAllBuckets()) {
			if (!assignedBucketNumbers.contains(bucket.getBucket())) {
				log.warn("Found Bucket[{}] in Storage that does not belong to this Worker according to the Worker information.", bucket.getId());
			}
			registerBucket(bucket, entity2Bucket, tableBuckets);
		}

		for (CBlock cBlock : storage.getAllCBlocks()) {
			registerCBlock(cBlock, connectorCBlocks);
		}

		return new BucketManager(worker.getJobManager(), storage, worker, entity2Bucket, connectorCBlocks, tableBuckets, entityBucketSize);
	}

	/**
	 * register entities, and create query specific indices for bucket
	 */
	private static void registerBucket(Bucket bucket, Object2IntMap<String> entity2Bucket, Map<Table, Int2ObjectMap<List<Bucket>>> tableBuckets) {
		for (String entity : bucket.entities()) {

			if(entity2Bucket.containsKey(entity)){
				// This is an unrecoverable state, but should not happen in practice. Just a precaution.
				assert entity2Bucket.getInt(entity) == bucket.getBucket();
				continue;
			}

			entity2Bucket.put(entity, bucket.getBucket());
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

	public Locked acquireLock(Connector connector) {
		return cBlockLocks.acquire(connector.getId());
	}

	@SneakyThrows
	public void fullUpdate() {
		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		final Collection<Concept<?>> allConcepts = storage.getAllConcepts();

		log.info("BEGIN full update for {} concepts.", allConcepts.size());

		for (Concept<?> c : allConcepts) {
			if (!(c instanceof TreeConcept)) {
				continue;
			}
			for (ConceptTreeConnector con : ((TreeConcept) c).getConnectors()) {
				for (Bucket bucket : storage.getAllBuckets()) {

					final CBlockId cBlockId = new CBlockId(bucket.getId(), con.getId());

					if (!con.getTable().equals(bucket.getTable())) {
						continue;
					}

					if (hasCBlock(cBlockId)) {
						log.trace("Skip calculation of CBlock[{}], because it was loaded from the storage.", cBlockId);
						continue;
					}

					log.trace("CBlock[{}] missing in Storage. Queuing recalculation", cBlockId);
					job.addCBlock(bucket, con);
				}
			}
		}

		if (!job.isEmpty()) {
			jobManager.addSlowJob(job);
		}
	}

	public boolean hasCBlock(CBlockId id) {
		return storage.getCBlock(id) != null;
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		registerCBlock(cBlock, connectorToCblocks);
	}

	public void addBucket(Bucket bucket) {
		storage.addBucket(bucket);
		registerBucket(bucket, entity2Bucket, tableToBuckets);
	}

	public void removeTable(Table table) {
		final Int2ObjectMap<List<Bucket>> removed = tableToBuckets.remove(table);

		// It's possible no buckets were registered yet
		if (removed != null) {
			removed.values()
				   .stream()
				   .flatMap(List::stream)
				   .forEach(this::removeBucket);
		}

		storage.removeTable(table.getId());
	}

	public void removeBucket(Bucket bucket) {
		storage.getAllCBlocks()
			   .stream()
			   .filter(cblock -> cblock.getBucket().equals(bucket))
			   .forEach(this::removeCBlock);

		tableToBuckets.getOrDefault(bucket.getTable(), Int2ObjectMaps.emptyMap())
					  .getOrDefault(bucket.getBucket(), Collections.emptyList())
					  .remove(bucket);

		storage.removeBucket(bucket.getId());
	}

	private void removeCBlock(CBlock cBlock) {

		connectorToCblocks.getOrDefault(cBlock.getConnector(), Int2ObjectMaps.emptyMap())
						  .getOrDefault(cBlock.getBucket().getBucket(), Collections.emptyMap())
						  .values()
						  .remove(cBlock);

		storage.removeCBlock(cBlock.getId());
	}

	public Set<String> getEntities() {
		return Collections.unmodifiableSet(entity2Bucket.keySet());
	}

	private int getBucket(String id) {
		return entity2Bucket.getInt(id);
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

	public List<Bucket> getEntityBucketsForTable(Entity entity, Table table) {
		final int bucketId = getBucket(entity.getId());

		return tableToBuckets.getOrDefault(table, Int2ObjectMaps.emptyMap())
							 .getOrDefault(bucketId, Collections.emptyList());
	}

	/**
	 * Collects all Entites, that have any of the concepts on the connectors in a specific time.
	 */
	public Set<String> getEntitiesWithConcepts(Collection<ConceptElement<?>> concepts, Set<Connector> connectors, CDateSet restriction) {
		final long requiredBits = ConceptNode.calculateBitMask(concepts);

		final Set<String> out = new HashSet<>();

		for (Connector connector : connectors) {
			if (!connectorToCblocks.containsKey(connector)) {
				continue;
			}

			for (Map<Bucket, CBlock> bucketCBlockMap : connectorToCblocks.get(connector).values()) {
				for (CBlock cblock : bucketCBlockMap.values()) {
					for (String entity : cblock.getBucket().entities()) {

						if (cblock.isConceptIncluded(entity, requiredBits) && restriction.intersects(cblock.getEntityDateRange(entity))) {
							out.add(entity);
						}
					}
				}
			}
		}

		return out;
	}

	public Map<Bucket, CBlock> getEntityCBlocksForConnector(Entity entity, Connector connector) {
		final int bucketId = getBucket(entity.getId());

		return connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap())
								 .getOrDefault(bucketId, Collections.emptyMap());
	}

	public boolean hasEntityCBlocksForConnector(Entity entity, Connector connector) {
		final int bucketId = getBucket(entity.getId());
		final Map<Bucket, CBlock> cblocks = connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap())
															  .getOrDefault(bucketId, Collections.emptyMap());

		for (Bucket bucket : cblocks.keySet()) {
			if (bucket.containsEntity(entity.getId())) {
				return true;
			}
		}

		return false;
	}

	public void updateConcept(Concept<?> incoming) {
		final Concept<?> prior = storage.getConcept(incoming.getId());
		if (prior != null) {
			removeConcept(prior);
		}

		addConcept(incoming);
	}

	public void removeConcept(Concept<?> concept) {

		// Just drop all CBlocks at once for the connectors
		for (Connector connector : concept.getConnectors()) {
			final Int2ObjectMap<Map<Bucket, CBlock>> removed = connectorToCblocks.remove(connector);

			// It's possible that no data has been loaded yet
			if (removed != null) {
				removed.values().stream()
					   .map(Map::values)
					   .flatMap(Collection::stream)
					   .map(CBlock::getId)
					   .forEach(storage::removeCBlock);
			}
		}

		storage.removeConcept(concept.getId());
	}

	public void addConcept(Concept<?> concept) {
		storage.updateConcept(concept);

		if (!(concept instanceof TreeConcept)) {
			return;
		}

		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		for (ConceptTreeConnector connector : ((TreeConcept) concept).getConnectors()) {

			for (Bucket bucket : storage.getAllBuckets()) {
				if (!bucket.getTable().equals(connector.getTable())) {
					continue;
				}

				final CBlockId cBlockId = new CBlockId(bucket.getId(), connector.getId());

				if (hasCBlock(cBlockId)) {
					continue;
				}

				job.addCBlock(bucket, connector);
			}
		}
		jobManager.addSlowJob(job);
	}


}