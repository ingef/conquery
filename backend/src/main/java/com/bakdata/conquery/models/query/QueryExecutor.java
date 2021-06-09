package com.bakdata.conquery.models.query;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryExecutor implements Closeable {

	private final ThreadPoolExecutor executor;
	private final ListeningExecutorService pool;
	
	public QueryExecutor(ThreadPoolExecutor executor) {
		this.executor = executor;
		this.pool = MoreExecutors.listeningDecorator(executor);
	}

	public ShardResult execute(ManagedExecutionId executionId, QueryPlan<?> queryPlan, ShardResult result, QueryExecutionContext context) {
		Collection<Entity> entities = context.getBucketManager().getEntities().values();

		if(entities.isEmpty()) {
			log.warn("Entities for query {} are empty", executionId);
		}

		List<ListenableFuture<Optional<EntityResult>>> futures = new ArrayList<>();


		for (Entity entity : entities) {
			QueryJob queryJob = new QueryJob(context, queryPlan, entity);
			ListenableFuture<Optional<EntityResult>> submit = pool.submit(queryJob);
			futures.add(submit);
		}

		ListenableFuture<List<Optional<EntityResult>>> future = Futures.allAsList(futures);
		result.setFuture(future);
		future.addListener(result::finish, MoreExecutors.directExecutor());

		return result;
	}

	@Override
	public void close() throws IOException {
		pool.shutdown();
		try {
			boolean success = pool.awaitTermination(1, TimeUnit.DAYS);
			if (!success && log.isDebugEnabled()) {
				log.error("Timeout has elapsed before termination completed for executor {}", pool);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean isBusy () {
		// This might not be super accurate (see the Documentation of ThreadPoolExecutor)
		return executor.getActiveCount() != 0 || !executor.getQueue().isEmpty();
	}
}
