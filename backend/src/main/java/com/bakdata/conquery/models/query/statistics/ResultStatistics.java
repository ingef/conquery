package com.bakdata.conquery.models.query.statistics;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

public record ResultStatistics(int entities, int total, List<ColumnStatsCollector.ResultColumnStatistics> statistics, Range<LocalDate> dateRange) {
	@NotNull
	public static ResultStatistics collectResultStatistics(ManagedQuery managedQuery, List<ResultInfo> resultInfos, Optional<ResultInfo> dateInfo, int dateIndex, PrintSettings printSettings, UniqueNamer uniqueNamer, ConqueryConfig conqueryConfig) {

		final List<ColumnStatsCollector>
				statsCollectors =
				resultInfos.stream()
						   .map(info -> ColumnStatsCollector.getStatsCollector(info, printSettings, info.getType(), uniqueNamer, conqueryConfig.getFrontend()))
						   .collect(Collectors.toList());

		final IntSet entities = new IntOpenHashSet();
		final AtomicInteger lines = new AtomicInteger();


		final AtomicReference<CDateRange> spanRef = new AtomicReference<>(null);
		final Consumer<Object[]> dateAggregator = getDateSpanner(dateInfo, dateIndex, spanRef);

		managedQuery.streamResults()
					.peek(result -> entities.add(result.getEntityId()))
					.map(EntityResult::listResultLines)
					.flatMap(List::stream)
					.forEach(line -> {

						dateAggregator.accept(line);

						lines.incrementAndGet();

						for (int col = 0; col < line.length; col++) {
							final ColumnStatsCollector collector = statsCollectors.get(col);
							if (collector == null) {
								continue;
							}

							collector.consume(line[col]);
						}
					});

		final List<ColumnStatsCollector.ResultColumnStatistics> columnStats = statsCollectors.stream()
																							 .filter(Objects::nonNull) // Not all columns produces stats
																							 .map(ColumnStatsCollector::describe)
																							 .toList();


		final Range<LocalDate> span = dateInfo.map(ignored -> spanRef.get().toSimpleRange()).orElse(CDateRange.all().toSimpleRange());
		return new ResultStatistics(entities.size(), lines.get(), columnStats, span);
	}

	/**
	 * If not dateInfo is given, don't try to span values. otherwise takes values from line at dateIndex, and handles them according to dateInfo.
	 */
	private static Consumer<Object[]> getDateSpanner(Optional<ResultInfo> dateInfo, int dateIndex, AtomicReference<CDateRange> spanRef) {
		if (dateInfo.isEmpty()) {
			return ignored -> {
			};
		}

		final ResultInfo info = dateInfo.get();

		final Consumer<CDateRange> spanner = date -> spanRef.getAndAccumulate(date, (old, incoming) -> incoming.spanClosed(old));

		final BiConsumer<Object, Consumer<CDateRange>> extractor = validityDateAggregator(info.getType());


		return line -> extractor.accept(line[dateIndex], spanner);

	}

	public static BiConsumer<Object, Consumer<CDateRange>> validityDateAggregator(ResultType dateType) {
		if (dateType instanceof ResultType.DateRangeT) {
			return (obj, con) -> con.accept(CDateRange.fromList((List<? extends Number>) obj));
		}


		if (dateType instanceof ResultType.DateT) {
			return (obj, con) -> con.accept(CDateRange.exactly((Integer) obj));
		}

		if (dateType instanceof ResultType.ListT listT) {
			final BiConsumer<Object, Consumer<CDateRange>> extractor = validityDateAggregator(listT.getElementType());
			return (obj, con) -> ((List<?>) obj).forEach(date -> extractor.accept(date, con));
		}

		throw new IllegalStateException("Unexpected date Type %s".formatted(dateType));
	}


}
