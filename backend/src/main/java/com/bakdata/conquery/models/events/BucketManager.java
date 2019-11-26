package com.bakdata.conquery.models.events;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.IdMutex.Locked;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.jobs.CalculateCBlocksJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Worker;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;

import java.util.Optional;

public class BucketManager {

	private final IdMutex<ConnectorId> cBlockLocks = new IdMutex<>();
	private final JobManager jobManager;
	private final WorkerStorage storage;
	private final Worker worker;
	private final IdMap<ConceptId, Concept<?>> concepts = new IdMap<>();
	private final IdMap<BucketId, Bucket> buckets = new IdMap<>();
	private final IdMap<CBlockId, CBlock> cBlocks = new IdMap<>();
	@Getter
	private final Int2ObjectMap<Entity> entities = new Int2ObjectAVLTreeMap<>();

	public BucketManager(JobManager jobManager, WorkerStorage storage, Worker worker) {
		this.jobManager = jobManager;
		this.storage = storage;
		this.worker = worker;
		this.concepts.addAll(storage.getAllConcepts());
		this.buckets.addAll(storage.getAllBuckets());
		this.cBlocks.addAll(storage.getAllCBlocks());

		jobManager.addSlowJob(new SimpleJob("Update Block Manager", this::fullUpdate));
	}

	private void fullUpdate() {
		for (Concept<?> c : concepts) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					Table t = con.getTable();
					CalculateCBlocksJob job = new CalculateCBlocksJob(storage, this, con, t);
					ConnectorId conName = con.getId();
					for (Import imp : t.findImports(storage)) {
						for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {
							BucketId bucketId = new BucketId(imp.getId(), bucketNumber);
							Optional<Bucket> bucket = buckets.getOptional(bucketId);
							if (bucket.isPresent()) {
								CBlockId cBlockId = new CBlockId(bucketId, conName);
								if (!cBlocks.getOptional(cBlockId).isPresent()) {
									job.addCBlock(imp, bucket.get(), cBlockId);
								}
							}
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
			entities
					.computeIfAbsent(entity, Entity::new)
					.addBucket(storage.getCentralRegistry().resolve(bucket.getImp().getTable()), bucket);
		}
	}

	private void registerCBlock(CBlock cBlock) {
		Bucket bucket = buckets.getOrFail(cBlock.getBucket());
		for (int entity : bucket) {
			entities
					.computeIfAbsent(entity, Entity::new)
					.addCBlock(
							storage.getCentralRegistry().resolve(cBlock.getConnector()),
							storage.getImport(cBlock.getBucket().getImp()),
							storage.getCentralRegistry().resolve(cBlock.getBucket().getImp().getTable()),
							bucket,
							cBlock
					);
		}
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
					for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

						BucketId bucketId = new BucketId(imp.getId(), bucketNumber);

						if (buckets.containsKey(bucketId)) {
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

			entity.removeBucket(bucket.getId());

			//TODO Verify that this is enough.
			if(entity.isEmpty()) {
				entities.remove(entityId);
			}
		}
	}

	private void deregisterCBlock(CBlockId cBlock) {
		Bucket bucket = buckets.getOrFail(cBlock.getBucket());
		for (int entityId : bucket) {
			final Entity entity = entities.get(entityId);

			if(entity == null)
				continue;

			entity.removeCBlock(cBlock.getConnector(), cBlock.getBucket());

			//TODO Verify that this is enough.
			if(entity.isEmpty()) {
				entities.remove(entityId);
			}
		}
	}

	public void removeBucket(BucketId bucketId) {
		Bucket bucket = buckets.remove(bucketId);
		if (bucket == null) {
			return;
		}

		deregisterBucket(bucket);

		for (Concept<?> c : concepts) {
			for (Connector con : c.getConnectors()) {
				try (Locked lock = cBlockLocks.acquire(con.getId())) {
					if (!con.getTable().getId().equals(bucket.getImp().getTable())) {
						continue;
					}

					CBlockId cBlockId = new CBlockId(
							bucketId,
							con.getId()
					);
					if (cBlocks.remove(cBlockId) != null) {
						storage.removeCBlock(cBlockId);
						deregisterCBlock(cBlockId);
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

		for (Connector con : c.getConnectors()) {

			try (Locked lock = cBlockLocks.acquire(con.getId())) {

				Table t = con.getTable();

				for (Import imp : t.findImports(storage)) {

					for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

						BucketId bucketId = new BucketId(imp.getId(), bucketNumber);
						Optional<Bucket> bucket = buckets.getOptional(bucketId);

						if (bucket.isEmpty()) {
							continue;
						}

						CBlockId cBlockId = new CBlockId(bucketId, con.getId());

						if (cBlocks.remove(cBlockId) != null) {
							storage.removeCBlock(cBlockId);
							deregisterCBlock(cBlockId);
						}
					}
				}
			}
		}
	}

	public void removeImport(ImportId imp) {

		for (int bucketNumber : worker.getInfo().getIncludedBuckets()) {

			BucketId bucketId = new BucketId(imp, bucketNumber);

			if (!buckets.containsKey(bucketId)) {
				continue;
			}

			for (Concept<?> concept : concepts.values()) {
				for (Connector con : concept.getConnectors()) {
					try (Locked lock = cBlockLocks.acquire(con.getId())) {
						CBlockId cBlockId = new CBlockId(bucketId, con.getId());

						if (cBlocks.remove(cBlockId) != null) {
							storage.removeCBlock(cBlockId);
							deregisterCBlock(cBlockId);
						}
					}
				}
			}

			deregisterBucket(buckets.get(bucketId));

			buckets.remove(bucketId);
			storage.removeBucket(bucketId);
		}
	}

	public boolean hasCBlock(CBlockId id) {
		return cBlocks.containsKey(id);
	}
}
