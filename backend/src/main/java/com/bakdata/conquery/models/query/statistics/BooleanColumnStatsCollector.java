package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Map;

import c10n.C10N;
import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;

@Getter
public class BooleanColumnStatsCollector extends ColumnStatsCollector {

	private int trues = 0;
	private int falses = 0;
	private int missing = 0;

	public BooleanColumnStatsCollector(String name, String label, String description, PrintSettings printSettings) {
		super(name, label, description, printSettings);
	}

	@Override
	public void consume(Object value) {
		if (value == null) {
			missing++;
			return;
		}

		if (((Boolean) value)) {
			trues++;
		}
		else {
			falses++;
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		final Results results = C10nCache.getLocalized(Results.class, getPrintSettings().getLocale());

		return new HistogramColumnDescription(
				getName(), getLabel(), getDescription(),
				List.of(
						new HistogramColumnDescription.Entry(results.True(), trues),
						new HistogramColumnDescription.Entry(results.False(), falses)
				),
				Map.of(
						C10N.get(StatisticsLabels.class, getPrintSettings().getLocale()).missing(),
						getPrintSettings().getIntegerFormat().format(getMissing())
				)
		);
	}

}
