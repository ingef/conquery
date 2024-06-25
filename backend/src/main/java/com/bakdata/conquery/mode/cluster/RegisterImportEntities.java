package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import lombok.Data;

/**
 * This class handles registration of entities. Relevant for counting and resolving entities from external sources.
 */
@Data
class RegisterImportEntities extends Job {
	private final List<Collection<String>> collectedEntities;
	private final DistributedNamespace namespace;
	private final ImportId importId;

	@Override
	public void execute() {
		// This task is quite slow, so be delay it as far as possible.
		for (Collection<String> entities : collectedEntities) {
			for (String entity : entities) {


				if (namespace.getStorage().containsEntity(entity)) {
					return;
				}

				namespace.getStorage().registerEntity(entity);
			}
		}
	}

	@Override
	public String getLabel() {
		return "Handle Bucket %s assignments.".formatted(importId);
	}
}
