package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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


		List<? extends ListenableFuture<?>> futures = selectFilters.stream()
																			 .map(SelectFilter::getColumn)
																			 .map(column ->
																						  jobsExecutorService.submit(() -> {
																							  final List<Bucket> buckets = table2Buckets.get(column.getTable());

																							  Set<String> values = buckets.stream()
																														  .flatMap(bucket -> ((StringStore) bucket.getStore(column)).streamValues())
																														  .collect(Collectors.toSet());
																							  context.send(new RegisterColumnValues(column, values, context.getInfo()
																																						   .getId()));
																						  })
																			 )
																			 .collect(Collectors.toList());


		ListenableFuture<List<Object>> all = Futures.allAsList(futures);

		// TODO await then submit WorkerDone
	}
}
