package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	private final int backpressure;
	@JsonIgnore @Getter
	private transient JobManagerStatus jobManagerStatus = new JobManagerStatus();
	@JsonIgnore
	private final transient Object jobManagerSync = new Object();

	public ShardNodeInformation(NetworkSession session, int backpressure) {
		super(session);
		this.backpressure = backpressure;
	}

	public void setJobManagerStatus(JobManagerStatus status) {
		this.jobManagerStatus = status;
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
