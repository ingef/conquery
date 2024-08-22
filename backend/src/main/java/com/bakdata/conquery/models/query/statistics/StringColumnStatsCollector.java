package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.Frequency;

@Getter
@Slf4j
public class StringColumnStatsCollector extends ColumnStatsCollector {

	private final Frequency frequencies = new Frequency();
	private final long limit;
	private final ResultPrinters.Printer printer;

	private int nulls = 0;


	public StringColumnStatsCollector(String name, String label, String description, ResultPrinters.Printer printer, PrintSettings printSettings, long limit) {
		super(name, label, description, printSettings);
		this.limit = limit;
		this.printer = printer;
	}

	@Override
	public void consume(Object value) {
		if (value == null) {
			nulls++;
			return;
		}

		// In case there's a mapping, we need to map the value
		final String printed = printer.print(value);
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

		final StatisticsLabels statisticsLabels = C10N.get(StatisticsLabels.class, getPrintSettings().getLocale());

		final Map<String, String> extras = new HashMap<>();

		if (entriesSorted.size() > limit) {
			extras.put(
					statisticsLabels.remainingValues(entriesSorted.size() - limit),
					statisticsLabels.remainingEntries(frequencies.getSumFreq() - shownTotal)
			);
		}

		extras.put(statisticsLabels.missing(), getPrintSettings().getIntegerFormat().format(getNulls()));

		return new HistogramColumnDescription(getName(), getLabel(), getDescription(), head, extras);
	}

}
