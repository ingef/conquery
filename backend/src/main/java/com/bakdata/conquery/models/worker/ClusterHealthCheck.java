package com.bakdata.conquery.models.worker;

import java.util.List;
import java.util.function.Predicate;

import com.bakdata.conquery.mode.cluster.ClusterState;
import com.codahale.metrics.health.HealthCheck;
import lombok.Data;

@Data
public class ClusterHealthCheck extends HealthCheck {

	private final ClusterState clusterState;

	@Override
	protected Result check() throws Exception {

		final List<String> disconnectedWorkers =
				clusterState.getShardNodes().values().stream()
							.filter(Predicate.not(ShardNodeInformation::isConnected))
							.map(ShardNodeInformation::toString)
							.toList();

		if (disconnectedWorkers.isEmpty()){
			return Result.healthy("All known shards are connected.");
		}

		return Result.unhealthy("The shard(s) %s are no longer connected.".formatted(String.join(",", disconnectedWorkers)));
	}
}
