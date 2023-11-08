package com.bakdata.conquery.apiv1.query.statistics;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.Getter;

@Getter
class DateColumnStatsCollector extends ColumnStatsCollector<Integer> {

	private final Map<String, Integer> quarterCounts = new HashMap<>();
	private final Map<String, Integer> monthCounts = new HashMap<>();

	private final AtomicInteger totalCount = new AtomicInteger();
	private final AtomicLong nulls = new AtomicLong(0);

	private final List<Number> samples = new ArrayList<>();

	private final BooleanSupplier samplePicker;

	public DateColumnStatsCollector(String name, String label, String description, String type, BooleanSupplier samplePicker) {
		super(name, label, description, type);
		this.samplePicker = samplePicker;
	}

	@Override
	public void consume(Integer value) {
		//TODO this is actually a CDateSet, so List[List[int]]
		totalCount.incrementAndGet();

		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		final LocalDate date = CDate.toLocalDate(value);
		final int year = date.getYear();
		final int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
		final int month = date.getMonthValue();

		final String yearQuarter = year + "-" + quarter;
		final String yearMonth = year + "-" + month;

		quarterCounts.compute(yearQuarter, (ignored, current) -> current == null ? 1 : current + 1);
		monthCounts.compute(yearMonth, (ignored, current) -> current == null ? 1 : current + 1);

		if (samplePicker.getAsBoolean()) {
			samples.add(value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		return new ColumnDescription(getName(), getLabel(), getDescription(), getType(),
									 totalCount.get(),
									 getNulls().intValue(),
									 quarterCounts, monthCounts,
									 CDateRange.all() //TODO span
		);
	}

	@Getter
	static class ColumnDescription extends ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final Map<String, Integer> quarterCounts;
		private final Map<String, Integer> monthCounts;

		private final CDateRange span;

		public ColumnDescription(String name, String label, String description, String type, int count, int nullValues,  Map<String, Integer> quarterCounts, Map<String, Integer> monthCounts, CDateRange span) {
			super(name, label, description, type);
			this.count = count;
			this.nullValues = nullValues;
			this.quarterCounts = quarterCounts;
			this.monthCounts = monthCounts;
			this.span = span;
		}
	}
}
