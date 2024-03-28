package com.bakdata.conquery.models.query.entity;

import com.bakdata.conquery.models.datasets.Table;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * All associated data to a single entity (usually a person), over all {@link Table}s and {@link com.bakdata.conquery.models.datasets.concepts.Concept}s.
 *
 * @implNote The ManagerNode does not hold any data of Entities, only the ShardNodes do (via Workers). Additionally, all data of a single Entity must be held by a single Worker only (See {@link  com.bakdata.conquery.models.worker.WorkerHandler#getResponsibleWorkerForBucket(int)}).
 */
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class Entity {
	private final String id;

}
