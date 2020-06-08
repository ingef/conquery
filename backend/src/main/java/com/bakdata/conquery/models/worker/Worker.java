package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {

	@Getter
	private final WorkerInformation info;

	@Getter
	private final JobManager jobManager;

	@Getter
	private final WorkerStorage storage;

	@Getter
	private final QueryExecutor queryExecutor;

	@Getter
	private final ThreadPoolExecutor pool;

	@Setter
	private NetworkSession session;


	public static Worker createWorker(WorkerInformation info, WorkerStorage storage, ConqueryConfig config) {
		final JobManager jobManager = new JobManager(info.getName());
		final BucketManager bucketManager = new BucketManager(jobManager, storage, info);

		storage.setBucketManager(bucketManager);
		jobManager.addSlowJob(new SimpleJob("Update Block Manager", bucketManager::fullUpdate));

		// Second format-str is used by threadpool.
		final ThreadPoolExecutor pool = config.getQueries().getExecutionPool().createService(String.format("Dataset[%s] Worker-Thread %d", info.getDataset()));
		final QueryExecutor queryExecutor = new QueryExecutor(MoreExecutors.listeningDecorator(pool));

		return new Worker(info, jobManager, storage, queryExecutor, pool);
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
		storage.close();
	}
	
	@Override
	public String toString() {
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}
}