package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import lombok.Getter;
import lombok.Setter;

public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	@Getter
	private final JobManager jobManager;
	@Getter
	private final WorkerStorage storage;
	@Getter
	private final QueryExecutor queryExecutor;
	@Getter
	private final WorkerInformation info;
	@Setter
	private NetworkSession session;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService executorService;
	
	public Worker(WorkerInformation info, JobManager jobManager, WorkerStorage storage, QueryExecutor queryExecutor, ExecutorService executorService) {
		this.info = info;
		this.jobManager = jobManager;
		this.storage = storage;
		this.executorService = executorService;
		BucketManager bucketManager = new BucketManager(jobManager, storage, info);
		storage.setBucketManager(bucketManager);
		this.queryExecutor = queryExecutor;
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MasterMessage transform(NamespaceMessage message) {
		return new ForwardToNamespace(info.getDataset(), message);
	}
	
	@Override
	public void close() throws IOException {
		queryExecutor.close();
		try {
			jobManager.close();
		}catch (Exception e) {
			log.error("Unable to close worker query executor of {}.", this, e);
		}
		storage.close();
	}
	
	@Override
	public String toString() {
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}
	public boolean isBusy() {
		return queryExecutor.isBusy();
	}
}