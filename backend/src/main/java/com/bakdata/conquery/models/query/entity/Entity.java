package com.bakdata.conquery.models.query.entity;

import com.bakdata.conquery.models.datasets.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * All associated data to a single entity (usually a person), over all {@link Table}s and {@link com.bakdata.conquery.models.datasets.concepts.Concept}s.
 *
 * @implNote The ManagerNode does not hold any data of Entities, only the ShardNodes do (via Workers). Additionally, all data of a single Entity must be held by a single Worker only (See {@link com.bakdata.conquery.models.worker.Namespace::getResponsibleWorker}).
 */
@RequiredArgsConstructor
@ToString(of = "id")
public class Entity {
	@Getter
	private final int id;


	/**
	 * Calculate the bucket of the {@link Entity::getId}. Used for distributing partitions of the data to {@link com.bakdata.conquery.models.worker.Worker}s
	 */
	public static int getBucket(int entityId, int entityBucketSize) {
		return entityId / entityBucketSize;
	}
}
