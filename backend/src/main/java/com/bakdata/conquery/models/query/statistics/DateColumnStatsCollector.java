package com.bakdata.conquery.models.query.statistics;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;

@Getter
public class DateColumnStatsCollector extends ColumnStatsCollector<Object> {

	private final SortedMap<String, Integer> quarterCounts = new TreeMap<>();
	private final SortedMap<String, Integer> monthCounts = new TreeMap<>();

	private final AtomicInteger totalCount = new AtomicInteger();
	private final AtomicLong nulls = new AtomicLong(0);
	private CDateRange span = null;

	public DateColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings) {
		super(name, label, description, type, printSettings);
	}

	@Override
	public void consume(Object value) {
		totalCount.incrementAndGet();

		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		final CDateRange dateRange = extractDateRange(getType(), value);
		span = dateRange.spanClosed(span);

		if (dateRange.isOpen()) {
			return;
		}

		for (int day = dateRange.getMinValue(); day <= dateRange.getMaxValue(); day++) {
			handleDay(day);
		}

	}

	private static CDateRange extractDateRange(ResultType dateType, Object dateValue) {
		if (dateType instanceof ResultType.DateRangeT) {
			return CDateRange.fromList((List<? extends Number>) dateValue);

		}

		if (dateType instanceof ResultType.DateT) {
			return CDateRange.exactly((Integer) dateValue);
		}


		throw new IllegalStateException("Unexpected type %s".formatted(dateType));
	}

	private void handleDay(int day) {
		final LocalDate date = CDate.toLocalDate(day);
		final int year = date.getYear();
		final int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
		final int month = date.getMonthValue();

		final String yearQuarter = year + "-" + quarter;
		// This code is pretty hot, therefore I want to avoid String.format
		final String yearMonth = year + "-" + (month < 10 ? "0" : "") + month;


		quarterCounts.compute(yearQuarter, (ignored, current) -> current == null ? 1 : current + 1);
		monthCounts.compute(yearMonth, (ignored, current) -> current == null ? 1 : current + 1);

	}

	@Override
	public ResultColumnStatistics describe() {

		return new ColumnDescription(getName(), getLabel(), getDescription(),
									 totalCount.get(),
									 getNulls().intValue(),
									 quarterCounts,
									 monthCounts,
									 span == null ? CDateRange.all().toSimpleRange() : span.toSimpleRange()
		);
	}

	@Getter
	@CPSType(id = "DATES", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final SortedMap<String, Integer> quarterCounts;
		private final SortedMap<String, Integer> monthCounts;

		private final Range<LocalDate> span;

		public ColumnDescription(String name, String label, String description, int count, int nullValues, SortedMap<String, Integer> quarterCounts, SortedMap<String, Integer> monthCounts, Range<LocalDate> span) {
			super(name, label, description);
			this.count = count;
			this.nullValues = nullValues;
			this.quarterCounts = quarterCounts;
			this.monthCounts = monthCounts;
			this.span = span;
		}
	}
}
