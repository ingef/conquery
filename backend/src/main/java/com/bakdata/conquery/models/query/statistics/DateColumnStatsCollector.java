package com.bakdata.conquery.models.query.statistics;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.ToString;

@Getter
public class DateColumnStatsCollector extends ColumnStatsCollector {

	private final Object2IntMap<String> monthCounts = new Object2IntOpenHashMap<>();

	private int totalCount = 0;
	private int nulls = 0;
	private final Function<Object, CDateRange> dateExtractor;
	private CDateRange span = null;


	public DateColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings) {
		super(name, label, description, printSettings);
		dateExtractor = getDateExtractor(type);
	}

	private static Function<Object, CDateRange> getDateExtractor(ResultType dateType) {
		if (dateType instanceof ResultType.DateRangeT) {
			return dateValue -> CDateRange.fromList((List<? extends Number>) dateValue);
		}

		if (dateType instanceof ResultType.DateT) {
			return dateValue -> CDateRange.exactly((Integer) dateValue);
		}

		throw new IllegalStateException("Unexpected type %s".formatted(dateType));
	}

	@Override
	public void consume(Object value) {
		totalCount++;

		if (value == null) {
			nulls++;
			return;
		}

		final CDateRange dateRange = dateExtractor.apply(value);
		span = dateRange.spanClosed(span);

		if (dateRange.isOpen()) {
			return;
		}

		for (int day = dateRange.getMinValue(); day <= dateRange.getMaxValue(); day++) {
			handleDay(day);
		}

	}

	private void handleDay(int day) {
		final LocalDate date = CDate.toLocalDate(day);
		final int year = date.getYear();
		final int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
		final int month = date.getMonthValue();

		// This code is pretty hot, therefore I want to avoid String.format
		final String yearMonth = year + "-" + (month < 10 ? "0" : "") + month;

		monthCounts.compute(yearMonth, (ignored, current) -> current == null ? 1 : current + 1);
	}

	@Override
	public ResultColumnStatistics describe() {

		return new ColumnDescription(getName(), getLabel(), getDescription(),
									 totalCount,
									 nulls,
									 new TreeMap<>(monthCounts),
									 span == null ? CDateRange.all().toSimpleRange() : span.toSimpleRange()
		);
	}

	@Getter
	@CPSType(id = "DATES", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final SortedMap<String, Integer> monthCounts;

		private final Range<LocalDate> span;

		public ColumnDescription(String name, String label, String description, int count, int nullValues, SortedMap<String, Integer> monthCounts, Range<LocalDate> span) {
			super(name, label, description);
			this.count = count;
			this.nullValues = nullValues;
			this.monthCounts = monthCounts;
			this.span = span;
		}
	}
}
