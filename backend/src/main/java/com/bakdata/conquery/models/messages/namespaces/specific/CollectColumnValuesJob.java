package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class CollectColumnValuesJob extends WorkerMessage {


	@Override
	public void react(Worker context) throws Exception {
		List<SelectFilter> selectFilters = context.getStorage().getAllConcepts().stream().map(Concept::getConnectors)
												  .flatMap(Collection::stream)
												  .map(Connector::getFilters)
												  .filter(SelectFilter.class::isInstance)
												  .map(SelectFilter.class::cast)
												  .toList();

		final Map<Table, List<Bucket>> table2Buckets = context.getStorage().getAllBuckets().stream()
												  .collect(Collectors.groupingBy(Bucket::getTable));


		ListeningExecutorService jobsExecutorService = MoreExecutors.listeningDecorator(context.getJobsExecutorService());


		Map<Column, ListenableFuture<Set<String>>> col2Values =
				selectFilters.stream()
							 .map(SelectFilter::getColumn)
							 .collect(Collectors.toMap(
									 Function.identity(),
									 column -> {
										 ListenableFuture<Set<String>> future = jobsExecutorService.submit(() -> {
											 final List<Bucket> buckets = table2Buckets.get(column.getTable());

											 return buckets.stream()
														   .flatMap(bucket -> ((StringStore) bucket.getStore(column)).streamValues())
														   .collect(Collectors.toSet());
										 });
										 return future;
									 }
							 ));

		col2Values.forEach((column, future) -> {
			future.addListener(() -> {
				try {
					context.send(new RegisterColumnValues(column, future.get()));
				}
				catch (InterruptedException e) {
					//TODO
					throw new RuntimeException(e);
				}
				catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			}, jobsExecutorService);
		})
		;



		ListenableFuture<List<Set<String>>> all = Futures.allAsList(col2Values.values());




	}
}
