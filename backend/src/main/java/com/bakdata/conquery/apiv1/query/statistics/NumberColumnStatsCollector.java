package com.bakdata.conquery.apiv1.query.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@Getter
class NumberColumnStatsCollector extends ColumnStatsCollector<Number> {
	private final DescriptiveStatistics statistics = new DescriptiveStatistics();
	private final AtomicLong nulls = new AtomicLong(0);

	private final List<Number> samples = new ArrayList<>();


	private final BooleanSupplier samplePicker;

	public NumberColumnStatsCollector(String name, String label, String description, ResultType type, BooleanSupplier samplePicker) {
		super(name, label, description, type);
		this.samplePicker = samplePicker;
	}

	@Override
	public void consume(Number value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}



		statistics.addValue(value.doubleValue());

		if (samplePicker.getAsBoolean()) {
			samples.add(value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		return new ColumnDescription(getName(), getLabel(), getDescription(), getType().toString(),
									 (int) (getStatistics().getN() + getNulls().get()),
									 getNulls().intValue(),
									 getStatistics().getMean(),
									 getStatistics().getPercentile(50d /*This is the median.*/),
									 getStatistics().getStandardDeviation(),
									 (int) getStatistics().getMin(),
									 (int) getStatistics().getMax(),
									 samples
		);
	}

	@Getter
	static class ColumnDescription extends ColumnStatsCollector.ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final double mean;
		private final double median;
		private final double stdDev;
		private final Number min;
		private final Number max;

		private final Collection<Number> samples;

		public ColumnDescription(String name, String label, String description, String type, int count, int nullValues, double mean, double median, double stdDev, Number min, Number max, Collection<Number> samples) {
			super(name, label, description, type);
			this.count = count;
			this.nullValues = nullValues;
			this.mean = mean;
			this.median = median;
			this.stdDev = stdDev;
			this.min = min;
			this.max = max;
			this.samples = samples;
		}
	}
}
