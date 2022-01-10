package com.bakdata.conquery.models.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	/**
	 * Threshold of jobs at which transmission of new messages will block for ManagerNode until below threshold.
	 */
	private final int backpressure;

	/**
	 * Contains latest state of the Job-Queue of the Shard.
	 *
	 * @implNote This is sent by the shards at regular intervals, not polled.
	 */
	@JsonIgnore
	@Getter
	private transient JobManagerStatus jobManagerStatus = new JobManagerStatus();

	/**
	 * Used to await/notify for full job-queues.
	 */
	@JsonIgnore
	private final transient Object jobManagerSync = new Object();

	public ShardNodeInformation(NetworkSession session, int backpressure) {
		super(session);
		this.backpressure = backpressure;

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
		return getJobManagerStatus().getTimestamp().until(LocalDateTime.now(), ChronoUnit.MILLIS);
	}

	@Override
	public void awaitClose() {
		super.awaitClose();
		SharedMetricRegistries.getDefault().remove(getLatenessMetricName());
	}

	public void setJobManagerStatus(JobManagerStatus status) {
		jobManagerStatus = status;
		if (status.size() < backpressure) {
			synchronized (jobManagerSync) {
				jobManagerSync.notifyAll();
			}
		}
	}

	public void waitForFreeJobqueue() throws InterruptedException {
		if (jobManagerStatus.size() >= backpressure) {
			log.trace("Have to wait for free JobQueue (size = {})", jobManagerStatus.size());
			synchronized (jobManagerSync) {
				jobManagerSync.wait();
			}
		}
	}
}
