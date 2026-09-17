package com.bakdata.conquery.models.events;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.RequiredArgsConstructor;
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


	/**
	 * The final Map is the way the APIs expect the data to be delivered.
	 * <p>
	 * Connector -> Bucket -> [BucketId -> CBlock]
	 */
	private final Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorToCblocks;

	/**
	 * Table -> BucketN -> [Buckets]
	 */
	private final Map<TableId, Int2ObjectMap<Set<BucketId>>> tableToBuckets;

	private final Map<ConceptId, Map<ImportId, ConceptTreeCache>> treeCaches = new ConcurrentHashMap<>();

	public static BucketManager create(Worker worker, WorkerStorage storage) {
		final Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorCBlocks = new HashMap<>();
		final Map<TableId, Int2ObjectMap<Set<BucketId>>> tableBuckets = new HashMap<>();

		final IntArraySet assignedBucketNumbers = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets that map to: {}", assignedBucketNumbers);

		log.info("BEGIN Register cblocks for {}", worker.getInfo().getId());

		try (Stream<CBlockId> cBlockIds = storage.getAllCBlockIds()) {
			cBlockIds.forEach(cBlockId -> {

				if (!assignedBucketNumbers.contains(cBlockId.getBucket().getBucket())) {
					log.warn("Found CBlock[{}] in Storage that does not belong to this Worker according to the Worker information.", cBlockId);
				}

				registerCblockToConnector(cBlockId, connectorCBlocks);
				assignBucketToTable(cBlockId.getBucket(), tableBuckets);
			});
		}

		// This avoids having to migrate or reimport for what is effectively known information.
		if (storage.getAllEntities().findAny().isEmpty() && storage.getAllBucketIds().findAny().isPresent()) {
			log.warn("Found no EntityToBuckets, initializing from Buckets");

			log.info("BEGIN assigning Buckets");
			Stopwatch stopwatch = Stopwatch.createStarted();
			storage.getAllBucketIds().forEach(bucketId -> {
				for (String entity : storage.getBucket(bucketId).entities()) {
					storage.addEntityToBucket(entity, bucketId.getBucket());
				}
			});

			log.debug("DONE assigning Buckets within {}", stopwatch);

		}


		log.debug("FINISHED Register cblocks for {}", worker.getInfo().getId());

		return new BucketManager(worker.getJobManager(), storage, worker, connectorCBlocks, tableBuckets);
	}

	private static void registerCblockToConnector(CBlockId cBlock, Map<ConnectorId, Int2ObjectMap<Map<BucketId, CBlockId>>> connectorCBlocks) {
		connectorCBlocks.computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
						.computeIfAbsent(cBlock.getBucket().getBucket(), bucketId -> new HashMap<>(3))
						.put(cBlock.getBucket(), cBlock);
	}

	private static void assignBucketToTable(BucketId bucketId, Map<TableId, Int2ObjectMap<Set<BucketId>>> tableBuckets) {
		tableBuckets.computeIfAbsent(bucketId.getImp().getTable(), id -> new Int2ObjectAVLTreeMap<>())
					.computeIfAbsent(bucketId.getBucket(), n -> new HashSet<>())
					.add(bucketId);
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		registerCblockToConnector(cBlock.getId(), connectorToCblocks);
	}

	public void addBucket(Bucket bucket) {
		storage.addBucket(bucket);

		assignBucketToTable(bucket.getId(), tableToBuckets);

		final CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, worker.getJobsExecutorService());

		try (Stream<Concept<?>> concepts = storage.getAllConcepts()) {
			concepts.filter(TreeConcept.class::isInstance)
					.flatMap(concept -> concept.getConnectors().stream())
					.filter(connector -> connector.resolveTableId().equals(bucket.getTable()))
					.filter(connector -> !hasCBlock(new CBlockId(bucket.getId(), connector.getId())))
					.forEach(connector -> job.addCBlock(bucket.getId(), (ConceptTreeConnector) connector));
		}

		for (String entity : bucket.entities()) {
			if (storage.hasEntity(entity)) {
				continue;
			}

			storage.addEntityToBucket(entity, bucket.getBucket());
		}


		if (job.isEmpty()) {
			return;
		}

		jobManager.addSlowJob(job);
	}

	public boolean hasCBlock(CBlockId id) {
		return storage.hasCBlock(id);
	}

	public void removeTable(TableId table) {
		final Int2ObjectMap<Set<BucketId>> removed = tableToBuckets.remove(table);

		// It's possible no buckets were registered yet
		if (removed != null) {
			removed.values().stream()
				   .flatMap(Set::stream)
				   .forEach(this::removeBucket);
		}

		storage.removeTable(table);
	}

	public void removeBucket(BucketId bucket) {
		try (Stream<CBlockId> cBlockIds = storage.getAllCBlockIds()) {
			cBlockIds.filter(cblock -> cblock.getBucket().equals(bucket))
					 .forEach(this::removeCBlock);
		}

		tableToBuckets.getOrDefault(bucket.getImp().getTable(), Int2ObjectMaps.emptyMap()).getOrDefault(bucket.getBucket(), Collections.emptySet()).remove(bucket);

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
		try (Stream<String> entities = storage.getAllEntities()) {
			return entities.collect(Collectors.toSet());
		}
	}

	/**
	 * Remove all buckets comprising the import. Which will in-turn remove all CBLocks.
	 */
	public void removeImport(ImportId imp) {
		try (Stream<BucketId> bucketIds = storage.getAllBucketIds()) {
			bucketIds.filter(bucket -> bucket.getImp().equals(imp)).forEach(this::removeBucket);
		}


		try (Stream<Concept<?>> concepts = storage.getAllConcepts()) {
			concepts.filter(TreeConcept.class::isInstance)
					.forEach(concept -> removeConceptTreeCacheByImport(concept.getId(), imp));
		}

		storage.removeImport(imp);
	}

	public void removeConceptTreeCacheByImport(ConceptId concept, ImportId imp) {
		Map<ImportId, ConceptTreeCache> treeCache = treeCaches.get(concept);

		if (treeCache == null) {
			// Not all concepts have a cache: only concepts with column-based connectors
			return;
		}

		treeCache.remove(imp);
	}

	public Set<BucketId> getEntityBucketsForTable(Entity entity, TableId table) {
		final int bucketId = getBucket(entity.getId());

		return tableToBuckets.getOrDefault(table, Int2ObjectMaps.emptyMap()).getOrDefault(bucketId, Collections.emptySet());
	}

	private int getBucket(String id) {
		return storage.getEntityBucket(id);
	}

	/**
	 * Collects all Entites, that have any of the concepts on the connectors in a specific time.
	 */
	public Set<String> getEntitiesWithConcepts(Collection<ConceptElementId<?>> concepts, Set<ConnectorId> connectors, CDateSet restriction) {
		List<ConceptElement<?>> resolvedConcepts =
				concepts.stream()
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
					CBlock cBlock = cBlockId.resolve();
					for (String entity : cBlock.getEntities()) {

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

		for (CBlockId cblock : cblocks.values()) {
			if (cblock.resolve().containsEntity(entity.getId())) {
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

			try (Stream<BucketId> allBuckets = storage.getAllBucketIds()) {
				allBuckets
						.filter(bucketId -> bucketId.getImp().getTable().equals(connector.resolveTableId()))
						.filter(bucketId -> !hasCBlock(new CBlockId(bucketId, connector.getId())))
						.forEach(bucket -> job.addCBlock(bucket, connector));
			}
		}
		jobManager.addSlowJob(job);
	}

	public void removeConceptTreeCacheByConcept(ConceptId concept) {
		treeCaches.remove(concept);
	}

	public ConceptTreeCache getConceptTreeCache(TreeConcept concept, ImportId imp) {
		return treeCaches.computeIfAbsent(concept.getId(), (ignored) -> new ConcurrentHashMap<>())
						 .computeIfAbsent(imp, (ignored) -> new ConceptTreeCache(concept));
	}

}
