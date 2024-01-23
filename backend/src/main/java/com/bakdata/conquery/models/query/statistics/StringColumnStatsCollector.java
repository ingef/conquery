package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
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
							 .sorted(Map.Entry.<Comparable<?>, Long>comparingByValue().reversed())
							 .toList();

		final long end = Math.min(limit, entriesSorted.size());

		final List<HistogramColumnDescription.Entry> head = new ArrayList<>();
		long shownTotal = 0;

		for (int i = 0; i < end; i++) {
			final Map.Entry<Comparable<?>, Long> counts = entriesSorted.get(i);

			final HistogramColumnDescription.Entry entry = new HistogramColumnDescription.Entry(((String) counts.getKey()), counts.getValue());
			head.add(entry);

			shownTotal += counts.getValue();

		}

		final Map<String, String> extras =
				entriesSorted.size() <= limit
				? Collections.emptyMap()
				: Map.of(C10N.get(StatisticsLabels.class, getPrintSettings().getLocale()).remainingNodes(entriesSorted.size() - limit), Long.toString(frequencies.getSumFreq() - shownTotal));

		return new HistogramColumnDescription(getName(), getLabel(), getDescription(), head, extras);
	}

}
