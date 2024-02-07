package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This Job collects the distinct values in the given columns and returns a {@link RegisterColumnValues} message for each column to the namespace on the manager.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "COLLECT_COLUMN_VALUES", base = NamespacedMessage.class)
public class CollectColumnValuesJob extends WorkerMessage implements ActionReactionMessage {

	@Getter
	@NsIdRefCollection
	private final Set<Column> columns;

	/**
	 * This exists only on the manager for the afterAllReaction.
	 */
	@JsonIgnore
	private final Namespace namespace;


	@Override
	public void react(Worker context) throws Exception {
		final Map<Table, List<Bucket>> table2Buckets = context.getStorage().getAllBuckets().stream()
															  .collect(Collectors.groupingBy(Bucket::getTable));


		final ListeningExecutorService jobsExecutorService = MoreExecutors.listeningDecorator(context.getJobsExecutorService());


		final List<? extends ListenableFuture<?>> futures =
				columns.stream()
					   .map(column ->
									jobsExecutorService.submit(() -> {
										final List<Bucket> buckets = table2Buckets.get(column.getTable());

										if (buckets == null) {
											log.debug("Skipping column '{}' because there are no buckets imported", column);
											return;
										}

										final Set<String> values = buckets.stream()
																		  .flatMap(bucket -> ((StringStore) bucket.getStore(column)).streamValues())
																		  .collect(Collectors.toSet());
										context.send(new RegisterColumnValues(getMessageId(), context.getInfo().getId(), column, values));
									})
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
				log.debug("Still waiting for jobs.");
			}
		}
	}

	@Override
	public void afterAllReaction() {
		log.debug("{} shrinking searches", this);
		final FilterSearch filterSearch = namespace.getFilterSearch();
		columns.forEach(filterSearch::shrinkSearch);
	}
}
