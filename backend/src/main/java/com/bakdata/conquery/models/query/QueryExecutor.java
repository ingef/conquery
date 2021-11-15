package com.bakdata.conquery.models.query;

import static com.bakdata.conquery.models.error.ConqueryError.asConqueryError;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class QueryExecutor implements Closeable {

	private final Worker worker;

	private final ThreadPoolExecutor executor;

	private final Set<ManagedExecutionId> cancelledQueries = new HashSet<>();

	public void unsetQueryCancelled(ManagedExecutionId query) {
		cancelledQueries.remove(query);
	}

	public void setQueryCancelled(ManagedExecutionId query) {
		cancelledQueries.add(query);
	}

	public boolean isCancelled(ManagedExecutionId query) {
		return cancelledQueries.contains(query);
	}

	public void sendFailureToManagerNode(ShardResult result, ConqueryError error) {
		result.finish(Collections.emptyList(), Optional.of(error), worker);
	}

	public boolean execute(Query query, QueryExecutionContext executionContext, ShardResult result) {
		Collection<Entity> entities = executionContext.getBucketManager().getEntities().values();
		ThreadLocal<QueryPlan<?>> plan = ThreadLocal.withInitial(() -> query.createQueryPlan(new QueryPlanContext(worker)));

		if (entities.isEmpty()) {
			log.warn("Entities for query are empty");
		}

		try {
			final List<CompletableFuture<Optional<EntityResult>>> futures =
					entities.stream()
							.map(entity -> new QueryJob(executionContext, plan, entity))
							.map(job -> CompletableFuture.supplyAsync(job, executor))
							.collect(Collectors.toList());

			final CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

			allDone.thenApply((ignored) -> futures.stream()
												  .map(CompletableFuture::join)
												  .flatMap(Optional::stream)
												  .collect(Collectors.toList()))
				   .whenComplete((results, exc) -> result.finish(Objects.requireNonNullElse(results, Collections.emptyList()), Optional.ofNullable(exc), worker));


			return true;
		}
		catch (Exception e) {
			// Just trace here. If it's wrapped as an UnknownError, it will be logged anyway.
			log.trace("Error while executing {}", executionContext.getExecutionId(), e);
			ConqueryError err = asConqueryError(e);
			sendFailureToManagerNode(result, asConqueryError(err));
			return false;
		}
	}


	@Override
	public void close() throws IOException {
		boolean success = MoreExecutors.shutdownAndAwaitTermination(executor, Duration.of(1, ChronoUnit.DAYS));
		if (!success && log.isDebugEnabled()) {
			log.error("Timeout has elapsed before termination completed for executor {}", executor);
		}
	}

	public boolean isBusy() {
		// This might not be super accurate (see the Documentation of ThreadPoolExecutor)
		return executor.getActiveCount() != 0 || !executor.getQueue().isEmpty();
	}
}
