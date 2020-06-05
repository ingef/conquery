package com.bakdata.conquery.models.query;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.config.ConqueryConfig;
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
	
	public QueryExecutor(ConqueryConfig config) {
		this.executor = config.getQueries().getExecutionPool().createService("Query Executor %d");
		this.pool = MoreExecutors.listeningDecorator(executor);
	}

	public ShardResult execute(ShardResult result, QueryExecutionContext context, Entry<ManagedExecutionId, QueryPlan> entry) {

		return execute(result, context, entry, pool);
	}

	public static ShardResult execute(ShardResult result, QueryExecutionContext context, Entry<ManagedExecutionId, QueryPlan> entry, ListeningExecutorService executor) {
		ManagedExecutionId executionId = entry.getKey();
		Collection<Entity> entries = context.getStorage().getBucketManager().getEntities().values();

		if(entries.isEmpty()) {
			log.warn("entries for query {} are empty", executionId);
		}
		
		List<ListenableFuture<EntityResult>> futures = entry.getValue()
			.executeOn(context, entries)
			.map(executor::submit)
			.collect(Collectors.toList());
		
		ListenableFuture<List<EntityResult>> future = Futures.allAsList(futures);
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
