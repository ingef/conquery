package com.bakdata.conquery.apiv1.query.statistics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import org.apache.commons.math3.stat.Frequency;

@Getter
public class StringColumnStatsCollector extends ColumnStatsCollector<String> {

	private final Frequency frequencies = new Frequency();
	private final AtomicLong nulls = new AtomicLong(0);

	public StringColumnStatsCollector(String name, String label, String description, ResultType type) {
		super(name, label, description, type);
	}

	@Override
	public void consume(String value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		frequencies.addValue(value);
	}

	@Override
	public ResultColumnStatistics describe() {
		final Map<String, Long> repr =
				StreamSupport.stream(((Iterable<Map.Entry<Comparable<?>, Long>>) frequencies::entrySetIterator).spliterator(), false)
							 .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue));


		return new ColumnDescription(getName(), getLabel(), getDescription(), getType().toString(), repr);
	}

	@Getter
	static class ColumnDescription extends ResultColumnStatistics {
		private final Map<String, Long> histogram;

		public ColumnDescription(String name, String label, String description, String type, Map<String, Long> histogram) {
			super(name, label, description, type);
			this.histogram = histogram;
		}
	}
}
