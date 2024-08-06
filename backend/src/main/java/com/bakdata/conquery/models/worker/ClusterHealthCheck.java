package com.bakdata.conquery.models.worker;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.bakdata.conquery.mode.cluster.ClusterState;
import com.codahale.metrics.health.HealthCheck;
import lombok.Data;

@Data
public class ClusterHealthCheck extends HealthCheck {

	public static final String HEALTHY_MESSAGE_FMT = "All %d known shards are connected.";
	private final ClusterState clusterState;

	@Override
	protected Result check() throws Exception {

		Collection<ShardNodeInformation> knownShards = clusterState.getShardNodes().values();
		final List<String> disconnectedWorkers =
				knownShards.stream()
						   .filter(Predicate.not(ShardNodeInformation::isConnected))
						   .map(ShardNodeInformation::toString)
						   .toList();

		if (disconnectedWorkers.isEmpty()){
			return Result.healthy(HEALTHY_MESSAGE_FMT, knownShards.size());
		}

		return Result.unhealthy("The shard(s) %s are no longer connected.".formatted(String.join(",", disconnectedWorkers)));
	}
}
