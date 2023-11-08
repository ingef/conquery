package com.bakdata.conquery.apiv1.query.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.google.common.math.StatsAccumulator;
import lombok.Getter;

@Getter
class NumberColumnStatsCollector extends ColumnStatsCollector<Number> {
	private final StatsAccumulator statistics = new StatsAccumulator();
	private final AtomicLong nulls = new AtomicLong(0);

	private final List<Number> samples = new ArrayList<>();

	private final BooleanSupplier samplePicker;

	public NumberColumnStatsCollector(String name, String label, String description, String type, BooleanSupplier samplePicker) {
		super(name, label, description, type);
		this.samplePicker = samplePicker;
	}

	@Override
	public void consume(Number value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		statistics.add((double) value);

		if (samplePicker.getAsBoolean()) {
			samples.add(value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		return new ColumnDescription(getName(), getLabel(), getDescription(), getType(),
												 (int) (getStatistics().count() + getNulls().get()),
												 getNulls().intValue(),
												 getStatistics().mean(),
												 getStatistics().sampleStandardDeviation(),
												 (int) getStatistics().min(),
												 (int) getStatistics().max()
		);
	}

	@Getter
	static class ColumnDescription extends ColumnStatsCollector.ResultColumnStatistics {

		private final int count;
		private final int nullValues;
		private final double mean;
		private final double stdDev;
		private final Number min;
		private final Number max;

		public ColumnDescription(String name, String label, String description, String type, int count, int nullValues, double mean, double stdDev, Number min, Number max) {
			super(name, label, description, type);
			this.count = count;
			this.nullValues = nullValues;
			this.mean = mean;
			this.stdDev = stdDev;
			this.min = min;
			this.max = max;
		}
	}
}
