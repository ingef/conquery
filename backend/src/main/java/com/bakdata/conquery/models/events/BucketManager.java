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
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @implNote This class is only used per {@link Worker}. And NOT in the ManagerNode.
 */
@Slf4j
public class BucketManager {

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;
	@Getter
	private final Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();
	//Backreference
	private final Worker worker;

	public BucketManager(JobManager jobManager, WorkerStorage storage, Worker worker) {
		this.jobManager = jobManager;
		this.storage = storage;
		this.worker = worker;
		
		IntArraySet requiredBuckets = worker.getInfo().getIncludedBuckets();
		log.trace("Trying to load these buckets: {}", requiredBuckets);
		for (Bucket bucket : storage.getAllBuckets()) {
			if(!requiredBuckets.contains(bucket.getBucket())) {
				log.warn("Found Bucket[{}] in Storage that does not belong to this Worker according to the Worker information.", bucket.getId());
			}
			else {
				requiredBuckets.remove(bucket.getBucket());
			}
			registerBucket(bucket);
		}
		if(!requiredBuckets.isEmpty()) {
			log.warn("Not all required Buckets were loaded from the storage. Missing Buckets: {}", requiredBuckets);
		}

		for (CBlock cBlock : storage.getAllCBlocks()) {
			registerCBlock(cBlock);
		}
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
								continue;
							}

							log.warn("CBlock[{}] missing in Storage.", cBlockId);

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
		Bucket bucket = storage.getBucket(cBlock.getBucket());
		if (bucket == null) {
			throw new NoSuchElementException("Could not find an element called '"+cBlock.getBucket()+"'");
		}
		for (int entity : bucket) {
			entities.computeIfAbsent(entity, createEntityFor(cBlock));
		}

		List<CBlock> forCBlock = connectorCBlocks
										 .computeIfAbsent(cBlock.getConnector(), connectorId -> new Int2ObjectAVLTreeMap<>())
										 .computeIfAbsent(bucket.getId().getBucket(), bucketId -> new ArrayList<>(3));

		forCBlock.add(cBlock);
	}

	public synchronized void addCalculatedCBlock(CBlock cBlock) {
		registerCBlock(cBlock);
	}

	public void addBucket(Bucket bucket) {
		storage.addBucket(bucket);
		registerBucket(bucket);

		for (Concept<?> c : storage.getAllConcepts()) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, con.getTable());
					Import imp = bucket.getImp();
					if (con.getTable().getId().equals(bucket.getImp().getTable())) {
						CBlockId cBlockId = new CBlockId(
								bucket.getId(),
								con.getId()
						);
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
						Bucket bucket = storage.getBucket(bucketId) ;

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

			if(entity == null)
				continue;

			if(isEntityEmpty(entity)) {
				entities.remove(entityId);
			}
		}
	}

	private void deregisterCBlock(CBlockId cBlockId) {
		Bucket bucket = storage.getBucket(cBlockId.getBucket());
		if (bucket == null) {
			throw new NoSuchElementException("Could not find an element called '"+cBlockId.getBucket()+"'");
		}
		for (int entityId : bucket) {
			final Entity entity = entities.get(entityId);

			if(entity == null)
				continue;

			//TODO Verify that this is enough.
			if(isEntityEmpty(entity)) {
				entities.remove(entityId);
			}
		}
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

	private void removeCBlock(CBlockId cBlockId) {
		if (storage.getCBlock(cBlockId) == null) {
			log.trace("Cannot remove CBlock[{}], because it is not present for worker[{}]", cBlockId, worker);
			return;
		}

		deregisterCBlock(cBlockId);

		storage.removeCBlock(cBlockId);
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
			if(!(concept instanceof TreeConcept))
				continue;

			((TreeConcept) concept).removeImportCache(imp);
		}
	}

	public boolean hasCBlock(CBlockId id) {
		return storage.getCBlock(id) != null;
	}

	public boolean hasBucket(BucketId id) {
		return storage.getBucket(id) != null;
	}

	private boolean hasBucket(int id) {
		return  storage.getAllBuckets().stream().map(Bucket::getBucket).anyMatch(bucket -> bucket == id);
	}

	public Bucket getBucket(BucketId id) {
		return storage.getBucket(id);
	}

	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId tableId) {
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		final List<Bucket> buckets = new ArrayList<>();

		for (Import imp : storage.getAllImports()) {
			if (!imp.getTable().equals(tableId)) {
				continue;
			}
			
			final Bucket bucket = getBucket(new BucketId(imp.getId(), bucketId));

			if(bucket == null){
				continue;
			}

			buckets.add(bucket);
		}

		return buckets;
	}

//	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId tableId) {
//		final int bucketId = Entity.getBucket(entity.getId(), bucketSize);
//		final Collection<ImportId> imports = storage.getTableImports(tableId);
//
//		final List<Bucket> buckets = new ArrayList<>();
//
//		for (ImportId impId : imports) {
//			final Bucket bucket = getBucket(new BucketId(impId, bucketId));
//
//			if(bucket == null){
//				continue;
//			}
//
//			buckets.add(bucket);
//		}
//
//		return buckets;
//	}


	/**
	 * Connector -> Bucket -> [CBlock]
	 * @implNote These numbers are estimates, we could make them configurable, though they aren't very important.
	 */
	private final Map<ConnectorId, Int2ObjectMap<List<CBlock>>> connectorCBlocks = new HashMap<>(150);


	public Map<BucketId, CBlock> getEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {

		final Int2ObjectMap<List<CBlock>> forConnector = connectorCBlocks.get(connectorId);

		if(forConnector == null){
			return Collections.emptyMap();
		}

		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		final List<CBlock> forBucket = forConnector.get(bucketId);

		if(forBucket == null){
			return Collections.emptyMap();
		}

		Map<BucketId, CBlock> out = new Object2ObjectArrayMap<>(forBucket.size());

		for (CBlock cBlock : forBucket) {
			out.put(cBlock.getBucket(), cBlock);
		}

		return out;
	}

	public boolean hasEntityCBlocksForConnector(Entity entity, ConnectorId connectorId) {

		final Int2ObjectMap<List<CBlock>> forConnector = connectorCBlocks.get(connectorId);
		final int bucketId = Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize());

		return forConnector != null && forConnector.containsKey(bucketId);
	}

	/**
	 * Test if there is any known associated data to the Entity in the {@link BucketManager}
	 * @param entity
	 */
	public boolean isEntityEmpty(Entity entity) {
		return !hasBucket(Entity.getBucket(entity.getId(), worker.getInfo().getEntityBucketSize()));
	}
}
