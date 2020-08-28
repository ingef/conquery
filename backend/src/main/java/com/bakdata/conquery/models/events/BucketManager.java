package com.bakdata.conquery.models.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.IdMap;
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
import com.bakdata.conquery.models.worker.WorkerInformation;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @implNote This class is only used per {@link Worker}. And NOT in the Master.
 */
@Slf4j
public class BucketManager {

	@Getter
	private final int bucketSize;

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;
	private final IdMap<ConceptId, Concept<?>> concepts = new IdMap<>();
	private final IdMap<BucketId, Bucket> buckets = new IdMap<>();
	private final IdMap<CBlockId, CBlock> cBlocks = new IdMap<>();
	@Getter
	private final Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();
	private final WorkerInformation workerInformation;



	public BucketManager(int bucketSize, JobManager jobManager, WorkerStorage storage, WorkerInformation workerInformation) {
		this.bucketSize = bucketSize;
		this.jobManager = jobManager;
		this.storage = storage;
		this.workerInformation = workerInformation;

		this.concepts.addAll(storage.getAllConcepts());
		this.buckets.addAll(storage.getAllBuckets());
		this.cBlocks.addAll(storage.getAllCBlocks());
	}

	@SneakyThrows
	public void fullUpdate() {
		for (Concept<?> c : concepts) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {

					Table t = con.getTable();
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
					ConnectorId conName = con.getId();

					for (Import imp : t.findImports(storage)) {

						storage.registerTableImport(imp.getId());

						for (int bucketNumber : workerInformation.getIncludedBuckets()) {
							BucketId bucketId = new BucketId(imp.getId(), bucketNumber);
							Optional<Bucket> bucket = buckets.getOptional(bucketId);

							if (bucket.isEmpty()) {
								continue;
							}

							CBlockId cBlockId = new CBlockId(bucketId, conName);

							if (cBlocks.getOptional(cBlockId).isPresent()) {
								continue;
							}

							job.addCBlock(imp, bucket.get(), cBlockId);
						}
					}

					if (!job.isEmpty()) {
						jobManager.addSlowJob(job);
					}
				}
			}
		}

		for (Bucket bucket : buckets) {
			registerBucket(bucket);
		}

		for (CBlock cBlock : cBlocks) {
			registerCBlock(cBlock);
		}
	}

	private void registerBucket(Bucket bucket) {
		for (int entity : bucket) {
			entities.computeIfAbsent(entity, createEntityFor(bucket));
		}
	}

	/**
	* Logic for tracing the creation of new Entities.
	*/
	private IntFunction<Entity> createEntityFor(Identifiable<?> idable) {

		return id -> {

			if(log.isDebugEnabled() && idable.getId() instanceof NamespacedId){
				byte[] thename = this.storage.getDictionary(ConqueryConstants.getPrimaryDictionary(((NamespacedId) idable.getId()).getDataset())).getElement(id);

				log.trace("Creating new Entitiy[{}]=`{}` for Bucket[{}]", id, new String(thename), idable.getId());
			}

			return new Entity(id);
		};
	}

	private void registerCBlock(CBlock cBlock) {
		Bucket bucket = buckets.getOrFail(cBlock.getBucket());
		for (int entity : bucket) {
			entities.computeIfAbsent(entity, createEntityFor(cBlock));
		}

		List<CBlock> forCBlock = connectorCBlocks
										 .computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
										 .computeIfAbsent(bucket.getId().getBucket(), bucketId -> new ArrayList<>(3));

		forCBlock.add(cBlock);
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		cBlocks.add(cBlock);
		registerCBlock(cBlock);
	}

	public void addBucket(Bucket bucket) {
		buckets.add(bucket);
		registerBucket(bucket);

		for (Concept<?> c : concepts) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, con.getTable());
					Import imp = bucket.getImp();
					if (con.getTable().getId().equals(bucket.getImp().getTable())) {
						CBlockId cBlockId = new CBlockId(
								bucket.getId(),
								con.getId()
						);
						if (!cBlocks.getOptional(cBlockId).isPresent()) {
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
		concepts.add(c);

		for (Connector con : c.getConnectors()) {
			try (Locked lock = cBlockLocks.acquire(con.getId())) {
				Table t = con.getTable();
				CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
				for (Import imp : t.findImports(storage)) {
					for (int bucketNumber : workerInformation.getIncludedBuckets()) {

						BucketId bucketId = new BucketId(imp.getId(), bucketNumber);

						if (!buckets.containsKey(bucketId)) {
							continue;
						}

						CBlockId cBlockId = new CBlockId(bucketId, con.getId());

						if (cBlocks.containsKey(cBlockId)) {
							continue;
						}

						job.addCBlock(imp, buckets.get(bucketId), cBlockId);
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

			if(entity == null)
				continue;

			if(entity.isEmpty(this)) {
				entities.remove(entityId);
			}
		}
	}

	private void deregisterCBlock(CBlockId cBlockId) {
		Bucket bucket = buckets.getOrFail(cBlockId.getBucket());

		for (int entityId : bucket) {
			final Entity entity = entities.get(entityId);

			if(entity == null)
				continue;

			//TODO Verify that this is enough.
			if(entity.isEmpty(this)) {
				entities.remove(entityId);
			}
		}

		final Int2ObjectMap<List<CBlock>> forConnector = connectorCBlocks.get(cBlockId.getConnector());

		if(forConnector == null){
			return;
		}

		final List<CBlock> forBucket = forConnector.get(cBlockId.getBucket().getBucket());

		if(forBucket == null){
			return;
		}

		forBucket.removeIf(cBlock -> cBlock.getId().equals(cBlockId));
	}

	public void removeBucket(BucketId bucketId) {
		Bucket bucket = buckets.get(bucketId);
		if (bucket == null) {
			return;
		}

		for (Concept<?> concept : concepts.values()) {
			for (Connector con : concept.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					removeCBlock(new CBlockId(bucketId, con.getId()));
				}
			}
		}

		deregisterBucket(buckets.get(bucketId));

		buckets.remove(bucketId);
		storage.removeBucket(bucketId);
	}

	private void removeCBlock(CBlockId cBlockId) {
		if (!cBlocks.containsKey(cBlockId)) {
			return;
		}

		deregisterCBlock(cBlockId);

		cBlocks.remove(cBlockId);
		storage.removeCBlock(cBlockId);
	}

	public void removeConcept(ConceptId conceptId) {
		Concept<?> c = concepts.get(conceptId);

		if (c == null) {
			return;
		}

		for (Connector con : c.getConnectors()) {
			for (Import imp : con.getTable().findImports(storage)) {
				for (int bucketNumber : workerInformation.getIncludedBuckets()) {

					BucketId bucketId = new BucketId(imp.getId(), bucketNumber);

					if (!buckets.containsKey(bucketId)) {
						continue;
					}

					removeCBlock(new CBlockId(bucketId, con.getId()));
				}
			}
		}

		concepts.remove(conceptId);
	}

	/**
	 * Remove all buckets comprising the import. Which will in-turn remove all CBLocks.
	 */
	public void removeImport(ImportId imp) {
		for (int bucketNumber : workerInformation.getIncludedBuckets()) {

			BucketId bucketId = new BucketId(imp, bucketNumber);

			if (!buckets.containsKey(bucketId)) {
				continue;
			}

			removeBucket(bucketId);
		}

		for (Concept<?> concept : concepts.values()) {
			if(!(concept instanceof TreeConcept))
				continue;

			((TreeConcept) concept).removeImportCache(imp);
		}
	}

	public boolean hasCBlock(CBlockId id) {
		return cBlocks.containsKey(id);
	}

	public boolean hasBucket(BucketId id) {
		return buckets.containsKey(id);
	}

	public boolean hasBucket(int id) {
		return  buckets.keySet().stream().map(BucketId::getBucket).anyMatch(bucket -> bucket == id);
	}

	public Bucket getBucket(BucketId id) {
		return buckets.get(id);
	}

	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId tableId) {
		final int bucketId = Entity.getBucket(entity.getId(), bucketSize);
		final Collection<ImportId> imports = storage.getTableImports(tableId);

		final List<Bucket> buckets = new ArrayList<>();

		for (ImportId impId : imports) {
			final Bucket bucket = getBucket(new BucketId(impId, bucketId));

			if(bucket == null){
				continue;
			}

			buckets.add(bucket);
		}

		return buckets;
	}


	/**
	 * Connector -> Bucket -> [CBlock]
	 * @implNote These numbers are estimates, we could make them configurable, though they aren't very important.
	 */
	private final Map<ConnectorId, Int2ObjectMap<List<CBlock>>> connectorCBlocks = new HashMap<>(150);

	public boolean hasEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {

		final Int2ObjectMap<List<CBlock>> forConnector = connectorCBlocks.get(connectorId);

		if(forConnector == null){
			return false;
		}

		final int bucketId = Entity.getBucket(entity.getId(), bucketSize);

		final List<CBlock> forBucket = forConnector.get(bucketId);

		return forBucket != null;
	}


	public Map<BucketId, CBlock> getEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {

		final Int2ObjectMap<List<CBlock>> forConnector = connectorCBlocks.get(connectorId);

		if(forConnector == null){
			return Collections.emptyMap();
		}

		final int bucketId = Entity.getBucket(entity.getId(), bucketSize);

		final List<CBlock> forBucket = forConnector.get(bucketId);

		if(forBucket == null){
			return Collections.emptyMap();
		}

		Map<BucketId, CBlock> out = new Object2ObjectArrayMap<>(forBucket.size());

		for (CBlock cBlock : this.cBlocks) {
			out.put(cBlock.getBucket(), cBlock);
		}

		return out;
	}
}
