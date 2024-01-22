package com.bakdata.conquery.models.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.util.ConcurrentHashSet;

@Slf4j
@ToString(callSuper = true)
public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	/**
	 * Threshold of jobs at which transmission of new messages will block for ManagerNode until below threshold.
	 */
	private final int backpressure;
	/**
	 * Used to await/notify for full job-queues.
	 */
	@JsonIgnore
	private final Object jobManagerSync = new Object();

	/**
	 * Contains latest state of the Job-Queue of the Shard.
	 *
	 * @implNote This is sent by the shards at regular intervals, not polled.
	 * @implNote This set is implemented as concurrent. Writers are single threaded, readers however are arbitrary.
	 */
	@JsonIgnore
	@Getter
	private final Set<JobManagerStatus> jobManagerStatus = new ConcurrentHashSet<>();
	private final AtomicBoolean full = new AtomicBoolean(false);
	private LocalDateTime lastStatusTime = LocalDateTime.now();

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


			final long pressure = calculatePressure();
			final boolean isFull = pressure > backpressure;

			full.set(isFull);

			if (!isFull) {
				synchronized (jobManagerSync) {
					jobManagerSync.notifyAll();
				}
			}
		}


	}

	private long calculatePressure() {
		return jobManagerStatus.stream().mapToLong(status -> status.getJobs().size()).sum();
	}

	public void waitForFreeJobQueue() throws InterruptedException {
		if (!full.get()) {
			return;
		}

		synchronized (jobManagerSync) {
			log.trace("Have to wait for free JobQueue");
			jobManagerSync.wait();
		}
	}
}
