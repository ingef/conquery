package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.mina.core.future.WriteFuture;

/**
 * This Job collects the distinct values in the given columns and returns a {@link RegisterColumnValues} message for each column to the namespace on the manager.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "COLLECT_COLUMN_VALUES", base = NamespacedMessage.class)
public class CollectColumnValuesJob extends WorkerMessage implements ActionReactionMessage {

	/**
	 * Trying to rate-limit how many threads are actively allocating column-values.
	 */
	private final int MAX_THREADS = Math.min(Runtime.getRuntime().availableProcessors(), 5);

	public final int columValueChunkSize;
	@Getter
	private final Set<ColumnId> columns;

	/**
	 * This exists only on the manager for the afterAllReaction.
	 */
	@JsonIgnore
	private final Namespace namespace;


	@Override
	public void react(Worker context) throws Exception {
		final Map<TableId, List<Bucket>> table2Buckets = context.getStorage().getAllBuckets()
																.collect(Collectors.groupingBy(Bucket::getTable));

		BasicThreadFactory threadFactory = (new BasicThreadFactory.Builder()).namingPattern(this.getClass().getSimpleName() + "-Worker-%d").build();
		final ListeningExecutorService jobsExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(MAX_THREADS, threadFactory));

		final AtomicInteger done = new AtomicInteger();

		final List<? extends ListenableFuture<?>> futures =
				columns.stream()
					   .filter(column -> table2Buckets.get(column.getTable()) != null)
					   .map(ColumnId::resolve)
					   .map(column -> {
								// Acquire before submitting, so we don't spam the executor with waiting threads
								return jobsExecutorService.submit(() -> {
									final List<Bucket> buckets = table2Buckets.get(column.getTable().getId());

									final Set<String> values = buckets.stream()
																	  .flatMap(bucket -> ((StringStore) bucket.getStore(column)).streamValues())
																	  .collect(Collectors.toSet());

									log.trace("Finished collecting {} values for column {}", values.size(), column);

									// Chunk values, to produce smaller messages
									Iterable<List<String>> partition = Iterables.partition(values, columValueChunkSize);

									log.trace("BEGIN Sending column values for {}. {} total values in {} sized batches",
											  column.getId(), values.size(), columValueChunkSize
									);

									int i = 0;
									for (List<String> chunk : partition) {
										// Send values to manager
										RegisterColumnValues message =
												new RegisterColumnValues(getMessageId(), context.getInfo().getId(), column.getId(), chunk);
										WriteFuture send = context.send(message);

										log.trace("Finished sending chunk {} for column '{}'", i++, column.getId());
									}

									getProgressReporter().report(1);
									log.trace("Finished collections values for column {} as number {}", column, done.incrementAndGet());
								});
							}
					   )
					   .collect(Collectors.toList());


		final ListenableFuture<List<Object>> all = Futures.allAsList(futures);

		while (true) {
			try {
				all.get(30, TimeUnit.SECONDS);
				break;
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			catch (TimeoutException e) {
				log.debug("Still waiting for jobs: {} of {} done", done.get(), futures.size());
			}
		}

		// We may do this, because we own this specific ExecutorService.
		jobsExecutorService.shutdown();
		getProgressReporter().done();

		log.info("Finished collecting values from these columns: {}", Arrays.toString(columns.toArray()));
		context.send(new FinalizeReactionMessage(getMessageId(), context.getInfo().getId()));
	}

	@Override
	public void afterAllReaction() {

		// Run this in a job, so it is definitely processed after UpdateFilterSearchJob
		namespace.getJobManager().addSlowJob(new SearchShrinker());
	}

	private class SearchShrinker extends Job {

		@Override
		public void execute() {

			final List<SelectFilter<?>> allSelectFilters = UpdateFilterSearchJob.getAllSelectFilters(namespace.getStorage());
			final FilterSearch filterSearch = namespace.getFilterSearch();

			getProgressReporter().setMax(allSelectFilters.size() + columns.size());

			log.debug("{} shrinking searches", this);

			for (ColumnId columnId : columns) {
				Column column = columnId.resolve();
				try {
					filterSearch.shrinkSearch(column);
				}
				catch (Exception e) {
					log.warn("Unable to shrink search for {}", column, e);
				}
				finally {
					getProgressReporter().report(1);
				}
			}

			log.info("BEGIN counting search totals on {}", namespace.getDataset().getId());

			for (SelectFilter<?> filter : allSelectFilters) {
				log.trace("Calculate totals for filter: {}", filter.getId());
				try {
					final long total = namespace.getFilterSearch().getTotal(filter);
					log.trace("Filter '{}' totals: {}", filter, total);
				}
				catch (Exception e) {
					log.warn("Unable to calculate totals for filter '{}'", filter.getId(), e);
				}
				finally {
					getProgressReporter().report(1);
				}
			}

			getProgressReporter().done();
			log.debug("FINISHED counting search totals on {}", namespace.getDataset().getId());
		}

		@Override
		public String getLabel() {
			return "Finalize Search update";
		}
	}
}
