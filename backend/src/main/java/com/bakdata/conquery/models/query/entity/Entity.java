package com.bakdata.conquery.models.query.entity;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * All associated data to a single entity (usually a person), over all {@link Table}s and {@link com.bakdata.conquery.models.concepts.Concept}s.
 *
 * @implNote The ManagerNode does not hold any data of Entities, only the Slaves do (via Workers). Additionally, all data of a single Entity must be held by a single Worker only (See {@link com.bakdata.conquery.models.worker.Namespace::getResponsibleWorker}).
 */
@RequiredArgsConstructor
@ToString(of = "id")
public class Entity {
	@Getter
	private final int id;
	private final ListMultimap<TableId, Bucket> buckets = ArrayListMultimap.create();
	private final HashBasedTable<ConnectorId, BucketId, EntityRow> cBlocks = HashBasedTable.create();

	public void addBucket(TableId id, Bucket bucket) {
		buckets.put(id, bucket);
	}

	public void addCBlock(Connector con, Import imp, Table table, Bucket bucket, CBlock cBlock) {
		if (cBlocks.put(con.getId(), bucket.getId(), new EntityRow(bucket, cBlock, con, imp, table)) != null) {
			throw new IllegalStateException("multiple CBlocks for block " + bucket + " & connector " + con);
		}
	}

	/**
	 * Test if there is any known associated data to the Entity.
	 */
	public boolean isEmpty() {
		return cBlocks.isEmpty() && buckets.isEmpty();
	}

	public void removeBucket(BucketId id) {
		buckets.values().removeIf(b -> b.getId().equals(id));
	}

	public void removeCBlock(ConnectorId connector, BucketId bucket) {
		cBlocks.remove(connector, bucket);
	}

	/**
	 * Calculate the bucket of the {@link Entity::getId}. Used for distributing partitions of the data to {@link com.bakdata.conquery.models.worker.Worker}s
	 */
	public static int getBucket(int entityId, int entityBucketSize) {
		return entityId / entityBucketSize;
	}

	public EntityRow getCBlock(ConnectorId connector, BucketId bucket) {
		return cBlocks.get(connector, bucket);
	}

	// TODO: 24.01.2020 What does this do?
	public Map<BucketId, EntityRow> getCBlockPreSelect(ConnectorId connector) {
		return cBlocks.row(connector);
	}

	public boolean hasConnector(ConnectorId connector) {
		return cBlocks.containsRow(connector);
	}

	/**
	 * Retrieve the {@link Bucket} containing this entity, for the Table.
	 */
	public List<Bucket> getBucket(TableId id) {
		return buckets.get(id);
	}
}
