package com.bakdata.conquery.models.query.statistics;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
public abstract class ColumnStatsCollector {
	private final String name;
	private final String label;
	private final String description;
	@JsonIgnore
	private final PrintSettings printSettings;

	public static ColumnStatsCollector getStatsCollector(String name, String description, ResultType type, ResultPrinters.Printer printer, PrintSettings printSettings, FrontendConfig config) {

		// List recursion must be done before assigning uniqueNames
		if (type instanceof ResultType.ListT<?> listT) {
			final ColumnStatsCollector columnStatsCollector = getStatsCollector(name, description, listT.getElementType(), ((ResultPrinters.ListPrinter) printer).elementPrinter(), printSettings, config);
			return new ListColumnStatsCollector(columnStatsCollector, printSettings);
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> new BooleanColumnStatsCollector(name, name, description, printSettings);
			case INTEGER, MONEY, NUMERIC -> new NumberColumnStatsCollector<>(name, name, description, type, printSettings, config.getVisualisationsHistogramLimit(), config.getVisualisationPercentiles().lowerEndpoint(), config.getVisualisationPercentiles().upperEndpoint());
			case DATE, DATE_RANGE -> new DateColumnStatsCollector(name, name, description, type, printSettings);
			case STRING -> new StringColumnStatsCollector(name, name, description, printer, printSettings, config.getVisualisationsHistogramLimit());
		};
	}

	public abstract void consume(@Nullable Object value);

	public abstract ResultColumnStatistics describe();

	@Data
	@CPSBase
	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "chart")
	public abstract static class ResultColumnStatistics {
		private final String name;
		private final String label;
		private final String description;

		protected ResultColumnStatistics(String name, String label, String description) {
			this.name = name;
			this.label = label;
			this.description = description;
		}
	}
}
