package com.bakdata.conquery.models.query;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class QueryExecutor implements Closeable {

	@Getter
	private final Queue<Runnable> jobs;

	public ShardResult execute(ShardResult result, QueryExecutionContext context, Entry<ManagedExecutionId, QueryPlan> entry) {

		return execute(result, context, entry, jobs);
	}

	public static ShardResult execute(ShardResult result, QueryExecutionContext context, Entry<ManagedExecutionId, QueryPlan> entry, Queue<Runnable> jobs) {
		ManagedExecutionId executionId = entry.getKey();
		Collection<Entity> entries = context.getStorage().getBucketManager().getEntities().values();

		if(entries.isEmpty()) {
			log.warn("entries for Query[{}] are empty", executionId);
		}

		List<ListenableFutureTask<EntityResult>> futures = entry.getValue()
																.executeOn(context, entries, result)
																.map(ListenableFutureTask::create)
																.collect(Collectors.toList());

		futures.forEach(jobs::offer);

		ListenableFuture<List<EntityResult>> future = Futures.allAsList(futures);
		result.setFuture(future);
		future.addListener(result::finish, MoreExecutors.directExecutor());
		return result;
	}

	@Override
	public void close() throws IOException {

	}
}
