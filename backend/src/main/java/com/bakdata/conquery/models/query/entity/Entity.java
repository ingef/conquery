package com.bakdata.conquery.models.query.entity;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.BucketManager;
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


	/**
	 * Test if there is any known associated data to the Entity in the {@link BucketManager}
	 * @param bucketManager
	 */
	public boolean isEmpty(BucketManager bucketManager) {
		return !bucketManager.hasBucket(getBucket(id, bucketManager.getBucketSize()));
	}


	/**
	 * Calculate the bucket of the {@link Entity::getId}. Used for distributing partitions of the data to {@link com.bakdata.conquery.models.worker.Worker}s
	 */
	public static int getBucket(int entityId, int entityBucketSize) {
		return entityId / entityBucketSize;
	}
	}
