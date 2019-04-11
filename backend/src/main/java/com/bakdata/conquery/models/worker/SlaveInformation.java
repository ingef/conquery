package com.bakdata.conquery.models.worker;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobStatus;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

public class SlaveInformation extends MessageSender.Simple<SlaveMessage> {
	@JsonIgnore @Getter
	private transient List<JobStatus> jobManagerStatus = Collections.emptyList();
	@JsonIgnore
	private final transient Object jobManagerSync = new Object();
	
	public SlaveInformation(NetworkSession session) {
		super(session);
	}
	
	public void setJobManagerStatus(List<JobStatus> status) {
		this.jobManagerStatus = status;
		if(status.size()<100) {
			synchronized (jobManagerSync) {
				jobManagerSync.notifyAll();
			}
		}
	}
	
	public void waitForFreeJobqueue() throws InterruptedException {
		if(jobManagerStatus.size()>=100) {
			synchronized (jobManagerSync) {
				jobManagerSync.wait();
			}
		}
	}
}
