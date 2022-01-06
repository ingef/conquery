package com.bakdata.conquery.models.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	private final int backpressure;
	@JsonIgnore
	@Getter
	private transient JobManagerStatus jobManagerStatus = new JobManagerStatus();
	@JsonIgnore
	private final transient Object jobManagerSync = new Object();

	private final Gauge latenessGauge;

	public ShardNodeInformation(NetworkSession session, int backpressure) {
		super(session);
		this.backpressure = backpressure;

		// This metric tracks when the last message from the corresponding shard was received.
		latenessGauge = SharedMetricRegistries.getDefault().gauge(
				getLatenessMetricName(),
				() -> this::getMillisSinceLastStatus
		);
	}

	private String getLatenessMetricName() {
		return String.join(".", "jobs", "latency", getRemoteAddress().toString());
	}

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
