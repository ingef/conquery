package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public class ShardNodeInformation extends MessageSender.Simple<MessageToShardNode> {
	private final int backpressure; //TODO FK: theres a name for that..
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
			synchronized (jobManagerSync) {
				jobManagerSync.wait();
			}
		}
	}
}
