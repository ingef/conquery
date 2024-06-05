package com.bakdata.conquery.models.query.statistics;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Slf4j
public record ResultStatistics(int entities, int total, List<ColumnStatsCollector.ResultColumnStatistics> statistics, Range<LocalDate> dateRange) {
	@SneakyThrows
	@NotNull
	public static ResultStatistics collectResultStatistics(SingleTableResult managedQuery, List<ResultInfo> resultInfos, Optional<ResultInfo> dateInfo, Optional<Integer> dateIndex, PrintSettings printSettings, UniqueNamer uniqueNamer, ConqueryConfig conqueryConfig) {


		//TODO pull inner executor service from ManagerNode
		final ListeningExecutorService
				executorService =
				MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1));

		// Yes, we are actually iterating the result for every job.

		// Span date-column
		final ListenableFuture<Range<LocalDate>> futureSpan;

		final boolean containsDates = dateInfo.isPresent();

		ExecutionManager<?> executionManager = printSettings.getNamespace().getExecutionManager();

		if (containsDates) {
			futureSpan = executorService.submit(() -> calculateDateSpan(managedQuery, dateInfo, dateIndex.get(), executionManager));
		}
		else {
			futureSpan = Futures.immediateFuture(CDateRange.all().toSimpleRange());
		}

		// Count result lines and entities (may differ in case of form or SecondaryIdQuery)
		final ListenableFuture<Integer> futureLines = executorService.submit(() -> (int) managedQuery.resultRowCount());

		final ListenableFuture<Integer> futureEntities =
				executorService.submit(() -> (int) managedQuery.streamResults(OptionalLong.empty(), executionManager).count());

		// compute ResultColumnStatistics for each column
		final List<ListenableFuture<ColumnStatsCollector.ResultColumnStatistics>>
				futureDescriptions =
				IntStream.range(0, resultInfos.size())
						 // If the query doesn't contain dates, we can skip the dates-column.
						 .filter(col -> !resultInfos.get(col).getSemantics().contains(new SemanticType.EventDateT()) || containsDates)
						 .mapToObj(col -> (Callable<ColumnStatsCollector.ResultColumnStatistics>) () -> {
							 final StopWatch started = StopWatch.createStarted();

							 final ResultInfo info = resultInfos.get(col);
							 final ColumnStatsCollector statsCollector =
									 ColumnStatsCollector.getStatsCollector(info, printSettings, info.getType(), uniqueNamer, conqueryConfig.getFrontend());

							 log.trace("BEGIN stats collection for {}", info);

							 managedQuery.streamResults(OptionalLong.empty(), executionManager)
										 .map(EntityResult::listResultLines)
										 .flatMap(List::stream)
										 .forEach(line -> statsCollector.consume(line[col]));

							 log.trace("DONE collecting values for {}, in {}", info, started);

							 final ColumnStatsCollector.ResultColumnStatistics description = statsCollector.describe();

							 log.debug("DONE description for {}, in {}", info, started);

							 return description;
						 })
						 .map(executorService::submit)
						 .toList();

		final Range<LocalDate> span = futureSpan.get();
		final List<ColumnStatsCollector.ResultColumnStatistics> descriptions = Futures.allAsList(futureDescriptions).get();
		final int lines = futureLines.get();
		final int entities = futureEntities.get();

		executorService.shutdown();

		return new ResultStatistics(entities, lines, descriptions, span);
	}

	private static Range<LocalDate> calculateDateSpan(SingleTableResult managedQuery, Optional<ResultInfo> dateInfo, int dateIndex, ExecutionManager<?> executionManager) {
		if (dateInfo.isEmpty()) {
			return CDateRange.all().toSimpleRange();
		}

		final AtomicReference<CDateRange> spanRef = new AtomicReference<>(null);
		final Consumer<Object[]> dateAggregator = getDateSpanner(dateInfo.get(), dateIndex, spanRef);

		managedQuery.streamResults(OptionalLong.empty(), executionManager).flatMap(EntityResult::streamValues).forEach(dateAggregator);

		final CDateRange span = spanRef.get();

		if (span == null) {
			return CDateRange.all().toSimpleRange();
		}

		return span.toSimpleRange();
	}

	/**
	 * If not dateInfo is given, don't try to span values. otherwise takes values from line at dateIndex, and handles them according to dateInfo.
	 */
	private static Consumer<Object[]> getDateSpanner(ResultInfo dateInfo, int dateIndex, AtomicReference<CDateRange> spanRef) {

		final Consumer<CDateRange> spanner = date -> spanRef.getAndAccumulate(date, (old, incoming) -> incoming.spanClosed(old));

		final BiConsumer<Object, Consumer<CDateRange>> extractor = validityDateExtractor(dateInfo.getType());

		return line -> extractor.accept(line[dateIndex], spanner);

	}

	public static BiConsumer<Object, Consumer<CDateRange>> validityDateExtractor(ResultType dateType) {
		if (dateType instanceof ResultType.DateRangeT) {
			return (obj, con) -> con.accept(CDateRange.fromList((List<? extends Number>) obj));
		}


		if (dateType instanceof ResultType.DateT) {
			return (obj, con) -> con.accept(CDateRange.exactly((Integer) obj));
		}

		if (dateType instanceof ResultType.ListT listT) {
			final BiConsumer<Object, Consumer<CDateRange>> extractor = validityDateExtractor(listT.getElementType());
			return (obj, con) -> ((List<?>) obj).forEach(date -> extractor.accept(date, con));
		}

		throw new IllegalStateException("Unexpected date Type %s".formatted(dateType));
	}


}
