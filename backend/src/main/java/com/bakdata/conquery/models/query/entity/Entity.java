package com.bakdata.conquery.models.query.entity;

import java.util.Map;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.google.common.collect.HashBasedTable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * All associated data to a single entity (usually a person), over all {@link Table}s and {@link com.bakdata.conquery.models.concepts.Concept}s.
 *
 * @implNote The master does not hold any data of Entities, only the Slaves do (via Workers). Additionally, all data of a single Entity must be held by a single Worker only (See {@link com.bakdata.conquery.models.worker.Namespace::getResponsibleWorker}).
 */
@RequiredArgsConstructor
@ToString(of = "id")
public class Entity {
	@Getter
	private final int id;
	private final HashBasedTable<ConnectorId, BucketId, CBlock> cBlocks = HashBasedTable.create();


	public void addCBlock(ConnectorId connectorId, BucketId bucketId, CBlock cBlock) {
		if (cBlocks.put(connectorId, bucketId, cBlock) != null) {
			throw new IllegalStateException(String.format("Multiple CBlocks for Bucket[%s]/Connector[%s]", bucketId, connectorId));
		}
	}

	/**
	 * Test if there is any known associated data to the Entity.
	 */
	public boolean isEmpty() {
		return cBlocks.isEmpty();
	}

	public void removeBucket(BucketId id) {
		// TODO: 04.08.2020 cleanup logic (for isEmpty) is still missing
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


	// TODO: 24.01.2020 What does this do?
	public Map<BucketId, CBlock> getCBlockPreSelect(ConnectorId connector) {
		return cBlocks.row(connector);
	}

	public boolean hasConnector(ConnectorId connector) {
		return cBlocks.containsRow(connector);
	}

}
