package com.bakdata.conquery.models.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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

	private final JobManager jobManager;
	private final WorkerStorage storage;

	private final Worker worker;

	private final Object2IntMap<String> entity2Bucket;


	/**
	 * The final Map is the way the APIs expect the data to be delivered.
	 * <p>
	 * Connector -> Bucket -> [BucketId -> CBlock]
	 */
	private final Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorToCblocks;

	/**
	 * Table -> BucketN -> [Buckets]
	 */
	private final Map<TableId, Int2ObjectMap<List<BucketId>>> tableToBuckets;

	@Getter
	private final int entityBucketSize;

	private final Map<ConceptId, Map<ImportId, ConceptTreeCache>> treeCaches = new ConcurrentHashMap<>();

	public static BucketManager create(Worker worker, WorkerStorage storage, int entityBucketSize) {
		final Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorCBlocks = new HashMap<>();
		final Map<TableId, Int2ObjectMap<List<BucketId>>> tableBuckets = new HashMap<>();
		final Object2IntMap<String> entity2Bucket = new Object2IntOpenHashMap<>();

		final IntArraySet assignedBucketNumbers = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets that map to: {}", assignedBucketNumbers);

		storage.getAllBuckets().forEach(bucket -> {
			log.trace("Processing bucket {}", bucket.getId());
			if (!assignedBucketNumbers.contains(bucket.getBucket())) {
				log.warn("Found Bucket[{}] in Storage that does not belong to this Worker according to the Worker information.", bucket.getId());
			}
			registerBucket(bucket, entity2Bucket, tableBuckets);
		});

		storage.getAllCBlocks().forEach(cBlock -> registerCBlock(cBlock, connectorCBlocks));

		return new BucketManager(worker.getJobManager(), storage, worker, entity2Bucket, connectorCBlocks, tableBuckets, entityBucketSize);
	}

	/**
	 * register entities, and create query specific indices for bucket
	 */
	private static void registerBucket(Bucket bucket, Object2IntMap<String> entity2Bucket, Map<TableId, Int2ObjectMap<List<BucketId>>> tableBuckets) {
		for (String entity : bucket.entities()) {

			if (entity2Bucket.containsKey(entity)) {
				// This is an unrecoverable state, but should not happen in practice. Just a precaution.
				assert entity2Bucket.getInt(entity) == bucket.getBucket();
				continue;
			}

			entity2Bucket.put(entity, bucket.getBucket());
		}

		tableBuckets.computeIfAbsent(bucket.getTable(), id -> new Int2ObjectAVLTreeMap<>())
					.computeIfAbsent(bucket.getBucket(), n -> new ArrayList<>())
					.add(bucket.getId());
	}

	/**
	 * Assert validity of operation, and create index for CBlocks.
	 */
	private static void registerCBlock(CBlock cBlock, Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorCBlocks) {
		connectorCBlocks.computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
						.computeIfAbsent(cBlock.getBucket().getBucket(), bucketId -> new HashMap<>(3))
						.put(cBlock.getBucket(), cBlock.getId());
	}


	@SneakyThrows
	public void fullUpdate() {
		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		storage.getAllConcepts().filter(TreeConcept.class::isInstance).flatMap(concept -> concept.getConnectors().stream().map(ConceptTreeConnector.class::cast))

			   .forEach(connector -> storage.getAllBucketIds().forEach(bucketId -> {

				   final CBlockId cBlockId = new CBlockId(bucketId, connector.getId());

				   if (!connector.getResolvedTableId().equals(bucketId.getImp().getTable())) {
					   return;
				   }

				   if (hasCBlock(cBlockId)) {
					   log.trace("Skip calculation of CBlock[{}], because it was loaded from the storage.", cBlockId);
					   return;
				   }

				   log.warn("CBlock[{}] missing in Storage. Queuing recalculation", cBlockId);
				   job.addCBlock(bucketId.resolve(), connector);
			   }));

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

		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		storage.getAllConcepts()
			   .filter(TreeConcept.class::isInstance)
			   .flatMap(concept -> concept.getConnectors().stream())
			   .filter(connector -> connector.getResolvedTableId().equals(bucket.getTable()))
			   .filter(connector -> !hasCBlock(new CBlockId(bucket.getId(), connector.getId())))
			   .forEach(connector -> job.addCBlock(bucket, (ConceptTreeConnector) connector));

		if (job.isEmpty()){
			return;
		}

		jobManager.addSlowJob(job);
	}

	public void removeTable(TableId table) {
		final Int2ObjectMap<List<BucketId>> removed = tableToBuckets.remove(table);

		// It's possible no buckets were registered yet
		if (removed != null) {
			removed.values().stream().flatMap(List::stream).forEach(this::removeBucket);
		}

		storage.removeTable(table);
	}

	public void removeBucket(BucketId bucket) {
		storage.getAllCBlockIds().filter(cblock -> cblock.getBucket().equals(bucket)).forEach(this::removeCBlock);

		tableToBuckets.getOrDefault(bucket.getImp().getTable(), Int2ObjectMaps.emptyMap()).getOrDefault(bucket.getBucket(), Collections.emptyList()).remove(bucket);

		storage.removeBucket(bucket);
	}

	private void removeCBlock(CBlockId cBlock) {

		connectorToCblocks.getOrDefault(cBlock.getConnector(), Int2ObjectMaps.emptyMap())
						  .getOrDefault(cBlock.getBucket().getBucket(), Collections.emptyMap())
						  .values()
						  .remove(cBlock);

		storage.removeCBlock(cBlock);
	}

	public Set<String> getEntities() {
		return Collections.unmodifiableSet(entity2Bucket.keySet());
	}

	/**
	 * Remove all buckets comprising the import. Which will in-turn remove all CBLocks.
	 */
	public void removeImport(ImportId imp) {
		storage.getAllBucketIds().filter(bucket -> bucket.getImp().equals(imp)).forEach(this::removeBucket);


		storage.getAllConcepts()
			   .filter(TreeConcept.class::isInstance)
			   .forEach(concept -> removeConceptTreeCacheByImport(concept.getId(), imp));

		storage.removeImport(imp);
	}

	public List<BucketId> getEntityBucketsForTable(Entity entity, TableId table) {
		final int bucketId = getBucket(entity.getId());

		return tableToBuckets.getOrDefault(table, Int2ObjectMaps.emptyMap()).getOrDefault(bucketId, Collections.emptyList());
	}

	private int getBucket(String id) {
		return entity2Bucket.getInt(id);
	}

	/**
	 * Collects all Entites, that have any of the concepts on the connectors in a specific time.
	 */
	public Set<String> getEntitiesWithConcepts(Collection<ConceptElementId<?>> concepts, Set<ConnectorId> connectors, CDateSet restriction) {
		List<ConceptElement<?>> resolvedConcepts = concepts.stream()
											   .<ConceptElement<?>>map(ConceptElementId::resolve)
											   .toList();

		final long requiredBits = ConceptNode.calculateBitMask(resolvedConcepts);

		final Set<String> out = new HashSet<>();

		for (ConnectorId connector : connectors) {
			if (!connectorToCblocks.containsKey(connector)) {
				continue;
			}

			for (Map<BucketId, CBlockId> bucketCBlockMap : connectorToCblocks.get(connector).values()) {
				for (CBlockId cBlockId : bucketCBlockMap.values()) {
					Bucket bucket = cBlockId.getBucket().resolve();

					for (String entity : bucket.entities()) {

						CBlock cBlock = cBlockId.resolve();
						if (cBlock.isConceptIncluded(entity, requiredBits) && restriction.intersects(cBlock.getEntityDateRange(entity))) {
							out.add(entity);
						}
					}
				}
			}
		}

		return out;
	}

	public Map<BucketId, CBlockId> getEntityCBlocksForConnector(Entity entity, ConnectorId connector) {
		final int bucketId = getBucket(entity.getId());

		return connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap()).getOrDefault(bucketId, Collections.emptyMap());
	}

	public boolean hasEntityCBlocksForConnector(Entity entity, ConnectorId connector) {
		final int bucketId = getBucket(entity.getId());
		final Map<BucketId, CBlockId> cblocks = connectorToCblocks.getOrDefault(connector, Int2ObjectMaps.emptyMap()).getOrDefault(bucketId, Collections.emptyMap());

		for (BucketId bucket : cblocks.keySet()) {
			if (bucket.resolve().containsEntity(entity.getId())) {
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
			final Int2ObjectMap<Map<BucketId, CBlockId>> removed = connectorToCblocks.remove(connector.getId());

			// It's possible that no data has been loaded yet
			if (removed != null) {
				removed.values().stream().map(Map::values).flatMap(Collection::stream).forEach(storage::removeCBlock);
			}
		}

		removeConceptTreeCacheByConcept(concept.getId());

		storage.removeConcept(concept.getId());
	}

	public void addConcept(Concept<?> concept) {
		storage.updateConcept(concept);

		if (!(concept instanceof TreeConcept)) {
			return;
		}

		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		for (ConceptTreeConnector connector : ((TreeConcept) concept).getConnectors()) {

			storage.getAllBuckets()
				   .filter(bucket -> bucket.getTable().equals(connector.getResolvedTableId()))
				   .filter(bucket -> !hasCBlock(new CBlockId(bucket.getId(), connector.getId())))
				   .forEach(bucket -> job.addCBlock(bucket, connector));
		}
		jobManager.addSlowJob(job);
	}


	public ConceptTreeCache getConceptTreeCache(TreeConcept concept, ImportId imp) {
		return treeCaches.computeIfAbsent(concept.getId(), (ignored) -> new ConcurrentHashMap<>()).computeIfAbsent(imp, (ignored) -> new ConceptTreeCache(concept));
	}

	public void removeConceptTreeCacheByImport(ConceptId concept, ImportId imp) {
		treeCaches.get(concept).remove(imp);
	}

	public void removeConceptTreeCacheByConcept(ConceptId concept) {
		treeCaches.remove(concept);
	}

}