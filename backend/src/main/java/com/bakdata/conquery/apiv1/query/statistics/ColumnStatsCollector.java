package com.bakdata.conquery.apiv1.query.statistics;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;

@Data
public abstract class ColumnStatsCollector<T> {
	private final String name;
	private final String label;
	private final String description;
	private final String type;

	public static ColumnStatsCollector getStatsCollector(ResultInfo info, final PrintSettings printSettings, BooleanSupplier samplePicker) {
		if (info.getType() instanceof ResultType.IntegerT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), info.getType()
																																						.toString(), samplePicker);
		}

		if (info.getType() instanceof ResultType.NumericT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), info.getType()
																																						.toString(), samplePicker);
		}

		if (info.getType() instanceof ResultType.MoneyT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), info.getType()
																																						.toString(), samplePicker);
		}


		return null; //TODO implement others
	}


	public abstract void consume(@Nullable T value);

	public abstract ResultColumnStatistics describe();

	@Data
	public static class ResultColumnStatistics {
		private final String name;
		private final String label;
		private final String description;
		private final String type;

		protected ResultColumnStatistics(String name, String label, String description, String type) {
			this.name = name;
			this.label = label;
			this.description = description;
			this.type = type;
		}
	}
}
