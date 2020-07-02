package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {

	@Getter
	private final WorkerInformation info;

	@Getter
	private final JobManager jobManager;

	@Getter
	private final WorkerStorage storage;

	@Getter
	private final QueryExecutor queryExecutor;

	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService executorService;

	@Setter
	private NetworkSession session;


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
		storage.close();
	}
	
	@Override
	public String toString() {
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}


	public boolean isBusy() {
		return jobManager.isSlowWorkerBusy();
	}
}