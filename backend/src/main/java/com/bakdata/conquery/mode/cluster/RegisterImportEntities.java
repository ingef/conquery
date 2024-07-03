package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import lombok.Data;

/**
 * This class handles registration of entities. Relevant for counting and resolving entities from external sources.
 */
@Data
class RegisterImportEntities extends Job {

	private final Map<Integer, Collection<String>> collectedEntities;


	private final DistributedNamespace namespace;
	private final ImportId importId;

	@Override
	public void execute() {
		// This task is quite slow, so be delay it as far as possible.
		for (Map.Entry<Integer, Collection<String>> bucket2Entities : collectedEntities.entrySet()) {
			for (String entity : bucket2Entities.getValue()) {

				if (namespace.getStorage().containsEntity(entity)) {
					continue;
				}

				namespace.getStorage().registerEntity(entity, bucket2Entities.getKey());
			}
		}
	}

	@Override
	public String getLabel() {
		return "Handle Bucket %s assignments.".formatted(importId);
	}
}
