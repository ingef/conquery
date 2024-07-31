package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Map;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import lombok.Getter;

@Getter
public class BooleanColumnStatsCollector extends ColumnStatsCollector {

	private int trues, falses, missing;

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
		return new HistogramColumnDescription(
				getName(), getLabel(), getDescription(),
				List.of(
						new HistogramColumnDescription.Entry(ResultPrinters.printBoolean(getPrintSettings(), true), trues),
						new HistogramColumnDescription.Entry(ResultPrinters.printBoolean(getPrintSettings(), false), falses)
				),
				Map.of(
						C10N.get(StatisticsLabels.class, getPrintSettings().getLocale()).missing(),
						getPrintSettings().getIntegerFormat().format(getMissing())
				)
		);
	}

}
