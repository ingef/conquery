package com.bakdata.conquery.models.query;

import static com.bakdata.conquery.models.error.ConqueryError.asConqueryError;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Data
public class QueryExecutor implements Closeable {

	private final Worker worker;

	private final ThreadPoolExecutor executor;

	private final int secondaryIdSubPlanLimit;

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

	public boolean execute(
		Query query,
		QueryExecutionContext executionContext,
		ShardResult result,
		Set<Entity> entities) {

		log.info("Received query: {}", query);

		Stopwatch stopwatch = Stopwatch.createStarted();
		final ThreadLocal<QueryPlan<?>> plan = ThreadLocal.withInitial(
			() -> query.createQueryPlan(new QueryPlanContext(executionContext.getStorage(), secondaryIdSubPlanLimit)));
		log.trace("Created query plan in {}", stopwatch);

		if (entities.isEmpty()) {
			// This is quite common for the entity preview, as only single entities are requested
			log.trace("Entities for query are empty");
		}

		try {
			// We log the QueryPlan once for debugging purposes.
			if (log.isDebugEnabled()) {
				log.debug("QueryPlan for Query[{}] = `{}`", result.getExecutionId(), plan.get());
			}

			final List<CompletableFuture<Optional<EntityResult>>> futures = entities.stream()
				.map(
					entity -> new QueryJob(executionContext, plan, entity))
				.map(
					job -> CompletableFuture.supplyAsync(job, executor))
				.toList();

			final CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

			// Wait for completion or an exception might be thrown from here
			// We could also use get(timeout) here to check cancellation state of the query
			allDone.join();

			// We are in the clear here, all futures/entities completed successfully
			List<EntityResult> entityResults = futures.stream()
				.map(CompletableFuture::join)
				.filter(
					Optional::isPresent)
				.map(Optional::get)
				.toList();

			// Prepare message to manager
			result.finish(entityResults, worker);

			sendResultToManagerNode(result);

			return true;
		} catch (Throwable e) {
			log.warn("Error while executing {}", executionContext.getExecutionId(), e);
			sendFailureToManagerNode(asConqueryError(e), executionContext.getExecutionId());
			return false;
		}
	}

	/**
	 * Send the {@link ShardResult} back to the manager
	 */
	private void sendResultToManagerNode(ShardResult result) {

		// Wrap in try-catch to catch errors in the mina filter chain
		try {
			WriteFuture sendResult = worker.send(result);


			// Add listener to handle errors that may arise after the filter chain
			sendResult.addListener(resultWriteFuture -> {

				WriteFuture wf = (WriteFuture) resultWriteFuture;
				if (wf.isWritten()) {
					log.trace("Successfully submitted shard result for execution {}", result.getExecutionId());
					return;
				}

				Throwable exception = wf.getException();

				sendFailureToManagerNode(exception, result.getExecutionId());
			});
		} catch (OutOfMemoryError oome) {
			// Result was too large for serialization
			throw new ConqueryError.ExecutionProcessingResultSizeError();
		}
	}

	public void sendFailureToManagerNode(Throwable throwable, ManagedExecutionId executionId) {
		ShardResult failMessage = new ShardResult(executionId, worker.getInfo().getId());
		failMessage.setError(ConqueryError.asConqueryError(throwable));
		failMessage.setResults(Collections.emptyList());

		WriteFuture sendFailure = worker.send(failMessage);
		sendFailure.addListener(logFailureTransmission(executionId));
	}

	@NotNull
	private static IoFutureListener<IoFuture> logFailureTransmission(ManagedExecutionId executionId) {
		return failWriteFuture -> {
			if (((WriteFuture) failWriteFuture).isWritten()) {
				log.info("Successfully informed manager about failed shard result for execution {}", executionId);
				return;
			}
			log.error(
				"Could not notify manager about shard result submission failure for execution {}. " + "Manager probably has this execution in a dangling {} state",
				executionId,
				ExecutionState.RUNNING
			);
		};
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
