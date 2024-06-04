package com.bakdata.conquery.mode.cluster;

import java.util.StringJoiner;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.codahale.metrics.health.HealthCheck;
import lombok.Data;

@Data
public class MatchingStatsHealthCheck extends HealthCheck {

	private final DatasetRegistry<DistributedNamespace> registry;
	private final ClusterState cluster;

	@Override
	protected Result check() throws Exception {

		final int expectedEntries = cluster.getShardNodes().size();

		final StringJoiner joiner = new StringJoiner(", ");

		for (DistributedNamespace namespace : registry.getDatasets()) {
			final NamespaceStorage storage = namespace.getStorage();

			for (Concept<?> concept : storage.getAllConcepts()) {
				if (concept.getMatchingStats() == null) {
					continue;
				}

				final int size = concept.getMatchingStats().getEntries().size();

				if (size == expectedEntries) {
					continue;
				}

				joiner.add("%s is missing %s entries.".formatted(concept.getId(), expectedEntries - size));
			}
		}

		if (joiner.length() == 0) {
			return Result.healthy();
		}

		return Result.unhealthy(joiner.toString());
	}
}
