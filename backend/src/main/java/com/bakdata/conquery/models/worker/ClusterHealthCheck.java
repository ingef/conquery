package com.bakdata.conquery.models.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.codahale.metrics.health.HealthCheck;
import lombok.Data;

@Data
public class ClusterHealthCheck extends HealthCheck {

	private final ClusterState clusterState;
	private final long ageMillis;

	@Override
	protected Result check() throws Exception {

		final List<String> disconnectedWorkers = new ArrayList<>();

		for (ShardNodeInformation shard : clusterState.getShardNodes().values()) {
			if (!shard.isConnected()) {
				disconnectedWorkers.add(shard.toString());
				continue;
			}

			// If we haven't received a message in a long while, the shard likely has connectivity issues.
			final boolean allLate = shard.getJobManagerStatus().stream()
										 .map(JobManagerStatus::getTimestamp)
										 .map(ts -> ts.until(LocalDateTime.now(), ChronoUnit.MILLIS))
										 .allMatch(elapsed -> elapsed > ageMillis);

			if (allLate) {
				disconnectedWorkers.add(shard.toString());
			}
		}

		if (disconnectedWorkers.isEmpty()) {
			return Result.healthy("All known shards are connected.");
		}

		return Result.unhealthy("The shard(s) %s are no longer connected.".formatted(String.join(",", disconnectedWorkers)));
	}
}
