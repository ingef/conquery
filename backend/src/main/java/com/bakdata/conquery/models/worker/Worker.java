package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
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
		final ThreadPoolExecutor pool = config.getQueries().getExecutionPool().createService(String.format("Dataset[%s] Worker-Thread %%d", info.getDataset()));

		//TODO fk: I am using a workstealing pool for the query-engine as that is probably exactly the use case for it. It could increase performance.
		final QueryExecutor queryExecutor = new QueryExecutor(MoreExecutors.listeningDecorator(Executors.newWorkStealingPool()));


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
		pool.shutdownNow();
		queryExecutor.close();
		storage.close();
	}
	
	@Override
	public String toString() {
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}

	public void awaitSubJobTermination() {
		do{
			Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			log.trace("{} active threads. {} remaining tasks.", getPool().getActiveCount(), getPool().getQueue().size());
		}while (getPool().getActiveCount() > 0);
	}
}