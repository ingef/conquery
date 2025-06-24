package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This Job collects the distinct values in the given columns and returns a {@link RegisterColumnValues} message for each column to the namespace on the manager.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "COLLECT_COLUMN_VALUES", base = NamespacedMessage.class)
public class CollectColumnValuesMessage extends WorkerMessage implements ActionReactionMessage {

	public final int columValueChunkSize;
	/**
	 * Trying to rate-limit how many threads are actively allocating column-values.
	 */
	private final int MAX_THREADS = Math.min(Runtime.getRuntime().availableProcessors(), 5);
	@NotNull
	@Getter
	private final Set<ColumnId> columns;

	/**
	 * This exists only on the manager for the afterAllReaction.
	 */
	@JsonIgnore
	private final Namespace namespace;

	@Override
	public void react(Worker context) throws Exception {
		context.getJobManager().addSlowJob(new CollectColumnValuesJob(context));
	}

	@RequiredArgsConstructor
	public class CollectColumnValuesJob extends Job {

		private final Worker context;

		@Override
		public void execute() throws Exception {
			final Map<TableId, List<BucketId>> table2Buckets;
			try (Stream<BucketId> allBuckets = context.getStorage().getAllBucketIds()) {
				table2Buckets = allBuckets
						.collect(Collectors.groupingBy(bucketId -> bucketId.getImp().getTable()));
			}

			final BasicThreadFactory threadFactory =
					new BasicThreadFactory.Builder()
							.namingPattern(getClass().getSimpleName() + "-%s".formatted(context.getInfo().getName()) + "-Worker-%d").build();

			final ListeningExecutorService jobsExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(MAX_THREADS, threadFactory));

			final AtomicInteger done = new AtomicInteger();

			getProgressReporter().setMax(columns.stream()
					.filter(column -> table2Buckets.get(column.getTable()) != null).count());


			log.info("BEGIN collecting values from {}", Arrays.toString(columns.toArray()));

			final List<? extends ListenableFuture<?>> futures =
					columns.stream()
							.filter(column -> table2Buckets.get(column.getTable()) != null)
							.map(ColumnId::resolve)
							.map(column -> jobsExecutorService.submit(() -> {
										final List<BucketId> buckets = table2Buckets.get(column.getTable().getId());

										final Set<String> values = buckets.stream()
												.map(BucketId::resolve)
												.flatMap(bucket -> ((StringStore) bucket.getStore(column)).streamValues())
												.collect(Collectors.toSet());

										log.trace("Finished collecting {} values for column {}", values.size(), column);

										// Chunk values, to produce smaller messages
										final Iterable<List<String>> partition = Iterables.partition(values, columValueChunkSize);

										log.trace("BEGIN Sending column values for {}. {} total values in {} sized batches",
												column.getId(), values.size(), columValueChunkSize
										);

										int i = 0;
										for (List<String> chunk : partition) {
											// Send values to manager
											final RegisterColumnValues message =
													new RegisterColumnValues(getMessageId(), context.getInfo().getId(), column.getId(), chunk);

											context.send(message);

											log.trace("Finished sending chunk {} for column '{}'", i++, column.getId());
										}

										getProgressReporter().report(1);
										log.trace("FINISH collections values for column {} as number {}", column, done.incrementAndGet());
									})
							)
							.toList();


			// We may do this, because we own this specific ExecutorService.
			jobsExecutorService.shutdown();

			while (!jobsExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
				log.debug("Still waiting for jobs on '{}': {} of {} done", context.getInfo().getName(), done.get(), futures.size());
			}

			log.info("FINISH collecting values from {}", Arrays.toString(columns.toArray()));
			context.send(new FinalizeReactionMessage(getMessageId(), context.getInfo().getId()));
		}


		@Override
		public String getLabel() {
			return this.getClass().getSimpleName();
		}
	}

	@Override
	public void afterAllReaction() {

		namespace.getJobManager().addSlowJob(namespace.getFilterSearch().createFinalizeFilterSearchJob(namespace, columns));
	}

}
