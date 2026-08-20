package com.bakdata.conquery.models.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	/**
	 * Contains latest state of the Job-Queue of the Shard.
	 *
	 * @implNote This is sent by the shards at regular intervals, not polled.
	 */
	@JsonIgnore
	private final Set<JobManagerStatus> jobManagerStatus = new HashSet<>();
	private LocalDateTime lastStatusTime = LocalDateTime.now();

	public ShardNodeInformation(NetworkSession session) {
		super(session);

		// This metric tracks when the last message from the corresponding shard was received.
		SharedMetricRegistries.getDefault().gauge(
				getLatenessMetricName(),
				() -> this::getMillisSinceLastStatus
		);
	}

	private String getLatenessMetricName() {
		return String.join(".", "jobs", "latency", getRemoteAddress().toString());
	}

	/**
	 * Calculate the time in Milliseconds since we last received a {@link JobManagerStatus} from the corresponding shard.
	 */
	private long getMillisSinceLastStatus() {
		return lastStatusTime.until(LocalDateTime.now(), ChronoUnit.MILLIS);
	}

	@Override
	public void awaitClose() {
		super.awaitClose();
		// ShardNode is being closed: We therefore remove the registered metric.
		SharedMetricRegistries.getDefault().remove(getLatenessMetricName());
	}

	public void addJobManagerStatus(JobManagerStatus incoming) {
		lastStatusTime = LocalDateTime.now();

		synchronized (jobManagerStatus) {
			// replace with new status
			jobManagerStatus.remove(incoming);
			jobManagerStatus.add(incoming);
		}


	}
}
