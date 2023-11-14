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
	private final ResultType type;

	public static ColumnStatsCollector getStatsCollector(ResultInfo info, final PrintSettings printSettings, BooleanSupplier samplePicker, ResultType type) {
		if (type instanceof ResultType.IntegerT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type, samplePicker);
		}

		if (type instanceof ResultType.NumericT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type, samplePicker);
		}

		if (type instanceof ResultType.MoneyT) {
			return new NumberColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type, samplePicker);
		}

		if (type instanceof ResultType.StringT) {
			return new StringColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type);
		}

		if (type instanceof ResultType.DateT) {
			return new DateColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type, samplePicker);
		}

		if (type instanceof ResultType.DateRangeT) {
			return new DateColumnStatsCollector(info.defaultColumnName(printSettings), info.userColumnName(printSettings), info.getDescription(), type, samplePicker);
		}

		if (type instanceof ResultType.ListT listT){
			final ColumnStatsCollector<?> columnStatsCollector = getStatsCollector(info, printSettings, samplePicker, listT.getElementType());
			// name label type are discarded when using ListColumnStatsCollector
			return new ListColumnStatsCollector<>(null, null, null, type, columnStatsCollector);
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
