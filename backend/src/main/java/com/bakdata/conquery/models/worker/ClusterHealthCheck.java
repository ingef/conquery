package com.bakdata.conquery.models.worker;

import java.net.SocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.codahale.metrics.health.HealthCheck;
import lombok.Data;

@Data
public class ClusterHealthCheck extends HealthCheck {

	public static final String HEALTHY_MESSAGE_FMT = "All %d known shards are connected.";
	private final ClusterState clusterState;
	private final Supplier<Collection<ShardNodeInformation>> nodeProvider;
	private final Duration heartbeatTimeout;

	@Override
	protected Result check() throws Exception {

		Collection<ShardNodeInformation> knownShards = clusterState.getShardNodes().values();
		final List<String> disconnectedWorkers =
				knownShards.stream()
						   .filter(Predicate.not(ShardNodeInformation::isConnected))
						   .map(ShardNodeInformation::toString)
						   .toList();

		if (!disconnectedWorkers.isEmpty()) {
			return Result.unhealthy("The shard(s) %s are no longer connected.".formatted(String.join(",", disconnectedWorkers)));
		}

		LocalDateTime now = LocalDateTime.now();
		List<ShardNodeInformation> timeoutShards = nodeProvider.get().stream()
															   .filter((status) -> heartbeatTimeout.minus(Duration.between(now, status.getLastStatusTime()))
																								   .isNegative()).toList();

		if (!timeoutShards.isEmpty()) {
			return Result.unhealthy("Shards timed out:%s".formatted(timeoutShards.stream()
																				 .map(ShardNodeInformation::getSession)
																				 .map(NetworkSession::getRemoteAddress)
																				 .map(SocketAddress::toString)
																				 .collect(Collectors.joining(", "))));
		}

		return Result.healthy(HEALTHY_MESSAGE_FMT, knownShards.size());
	}
}
