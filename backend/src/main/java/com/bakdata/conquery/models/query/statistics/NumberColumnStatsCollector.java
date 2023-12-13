package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@Getter
public class NumberColumnStatsCollector<TYPE extends Number & Comparable<TYPE>> extends ColumnStatsCollector<Number> {
	private final DescriptiveStatistics statistics = new DescriptiveStatistics();
	private final AtomicLong nulls = new AtomicLong(0);

	private final List<TYPE> samples = new ArrayList<>();


	private final BooleanSupplier samplePicker;

	private final Comparator<TYPE> comparator;

	public NumberColumnStatsCollector(String name, String label, String description, ResultType type, BooleanSupplier samplePicker, PrintSettings printSettings) {
		super(name, label, description, type, printSettings);
		this.samplePicker = samplePicker;
		this.comparator = selectComparator(type);
	}

	private Comparator<TYPE> selectComparator(ResultType resultType) {
		// The java type system was not made to handle the silliness, sorry.
		if (resultType instanceof ResultType.IntegerT) {
			return Comparator.comparingInt(Number::intValue);
		}

		if (resultType instanceof ResultType.NumericT) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		if (resultType instanceof ResultType.MoneyT) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		throw new IllegalArgumentException("Cannot handle result type %s".formatted(resultType.toString()));
	}

	@Override
	public void consume(Number value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		// TODO this feels like a pretty borked abstraction
		if (getType() instanceof ResultType.MoneyT moneyT) {
			value = moneyT.readIntermediateValue(getPrintSettings(), value);
		}

		statistics.addValue(value.doubleValue());

		if (samplePicker.getAsBoolean()) {
			samples.add((TYPE) value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		// If no real samples were collected, we short-circuit, as Statistics will throw an exception when empty.
		if (getStatistics().getN() == 0) {
			return new ColumnDescription(
					getName(), getLabel(), getDescription(), getType().toString(),
					getNulls().intValue(), getNulls().intValue(),
					Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0, Collections.emptyList()
			);
		}

		final double p99 = getStatistics().getPercentile(99d);
		final double maybeP01 = getStatistics().getPercentile(1d);

		// If min is basically 0, we don't prune for it, as those are usually relevant values.
		final double p01 = (Math.abs(maybeP01) < 2 * Double.MIN_VALUE) ? Double.MIN_VALUE : maybeP01;

		return new ColumnDescription(getName(), getLabel(), getDescription(), getType().toString(), (int) (getStatistics().getN() + getNulls().intValue()), getNulls().intValue(), getStatistics().getMean(), getStatistics().getPercentile(50d /*This is the median.*/), getStatistics().getStandardDeviation(), (int) getStatistics().getMin(), (int) getStatistics().getMax(),
									 // We cull extremes, as that can cause distortions when displayed.
									 getStatistics().getSum(), samples.stream().filter(val -> val.doubleValue() >= p01 && val.doubleValue() <= p99).sorted(comparator).toList()
		);
	}

	@Getter
	@CPSType(id = "DESCRIPTIVE", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ColumnStatsCollector.ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final double mean;
		private final double median;
		private final double stdDev;
		private final Number min;
		private final Number max;

		private final Number sum;

		private final Collection<? extends Number> samples;

		public ColumnDescription(String name, String label, String description, String type, int count, int nullValues, double mean, double median, double stdDev, Number min, Number max, Number sum, Collection<? extends Number> samples) {
			super(name, label, description, type);
			this.count = count;
			this.nullValues = nullValues;
			this.mean = mean;
			this.median = median;
			this.stdDev = stdDev;
			this.min = min;
			this.max = max;
			this.sum = sum;
			this.samples = samples;
		}
	}
}
