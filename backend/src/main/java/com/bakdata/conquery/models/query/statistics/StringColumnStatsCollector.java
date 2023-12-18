package com.bakdata.conquery.models.query.statistics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.Frequency;

@Getter
@Slf4j
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
		final List<Map.Entry<Comparable<?>, Long>> entriesSorted =
				StreamSupport.stream(((Iterable<Map.Entry<Comparable<?>, Long>>) frequencies::entrySetIterator).spliterator(), false)
							 .sorted(Map.Entry.<Comparable<?>, Long>comparingByValue().reversed()).toList();

		List<ColumnDescription.Entry> repr =
				entriesSorted.stream()
							 .limit(limit)
							 .map(entry -> new ColumnDescription.Entry(((String) entry.getKey()), entry.getValue()))
							 .toList();

		return new ColumnDescription(getName(), getLabel(), getDescription(), repr, Collections.emptyMap());
	}

	@Getter
	@CPSType(id = "HISTO", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ResultColumnStatistics {

		public static record Entry(String label, long value) {};
		private final List<Entry> entries;
		/*
		{
			histogram : [
				{
				  "label" : "E00-E99", "value": 10
				},
				{
				  "label" : "E00-E99", "value": 10
				},
				{
				  "label" : "E00-E99", "value": 10
				}
			],
			"extras" : {
				"Median" : 0.4,
		}
		 */

		private final Map<String, String> extras;

		public ColumnDescription(String name, String label, String description, List<Entry> histogram, Map<String, String> extras) {
			super(name, label, description, "STRING");
			this.entries = histogram;
			this.extras = extras;
		}
	}
}
