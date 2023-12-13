package com.bakdata.conquery.models.query.statistics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.math3.stat.Frequency;

@Getter
public class StringColumnStatsCollector extends ColumnStatsCollector<String> {

	private final Frequency frequencies = new Frequency();
	private final AtomicLong nulls = new AtomicLong(0);
	private final long limit;

	public StringColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings, long limit) {
		super(name, label, description, type, printSettings);
		this.limit = limit;
	}

	@Override
	public void consume(String value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		// In case there's a mapping, we need to map the value
		final String printed = getType().printNullable(getPrintSettings(), value);
		frequencies.addValue(printed);
	}

	@Override
	public ResultColumnStatistics describe() {
		final Map<String, Long> repr =
				StreamSupport.stream(((Iterable<Map.Entry<Comparable<?>, Long>>) frequencies::entrySetIterator).spliterator(), false)
							 .sorted(Map.Entry.<Comparable<?>, Long>comparingByValue().reversed())
							 .limit(limit)
							 .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue));


		return new ColumnDescription(getName(), getLabel(), getDescription(), repr);
	}

	@Getter
	@CPSType(id = "HISTO", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ResultColumnStatistics {
		private final Map<String, Long> histogram;

		public ColumnDescription(String name, String label, String description, Map<String, Long> histogram) {
			super(name, label, description, "STRING");
			this.histogram = histogram;
		}
	}
}
