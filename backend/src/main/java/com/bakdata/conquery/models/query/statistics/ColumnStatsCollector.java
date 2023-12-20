package com.bakdata.conquery.models.query.statistics;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
public abstract class ColumnStatsCollector<T> {
	private final String name;
	private final String label;
	private final String description;
	private final ResultType type;
	@JsonIgnore
	private final PrintSettings printSettings;

	public static ColumnStatsCollector getStatsCollector(ResultInfo info, final PrintSettings printSettings, ResultType type, UniqueNamer uniqueNamer, long displayLimit) {

		// List recursion must be done before assigning uniqueNames
		if (type instanceof ResultType.ListT listT){
			final ColumnStatsCollector<?> columnStatsCollector = getStatsCollector(info, printSettings, listT.getElementType(), uniqueNamer, displayLimit);
			// name label type are discarded when using ListColumnStatsCollector
			return new ListColumnStatsCollector<>(null, null, null, type, columnStatsCollector, printSettings);
		}


		final String name = uniqueNamer.getUniqueName(info);
		final String label = info.defaultColumnName(printSettings);

		if (type instanceof ResultType.IntegerT) {
			return new NumberColumnStatsCollector(name, label, info.getDescription(), type, printSettings);
		}

		if (type instanceof ResultType.NumericT) {
			return new NumberColumnStatsCollector(name, label, info.getDescription(), type, printSettings);
		}

		if (type instanceof ResultType.MoneyT) {
			return new NumberColumnStatsCollector(name, label, info.getDescription(), type, printSettings);
		}

		if (type instanceof ResultType.StringT) {
			return new StringColumnStatsCollector(name, label, info.getDescription(), type, printSettings, displayLimit);
		}

		if (type instanceof ResultType.BooleanT) {
			return new BooleanColumnStatsCollector(name, label, info.getDescription(), printSettings);
		}

		if (type instanceof ResultType.DateT) {
			return new DateColumnStatsCollector(name, label, info.getDescription(), type, printSettings);
		}

		if (type instanceof ResultType.DateRangeT) {
			return new DateColumnStatsCollector(name, label, info.getDescription(), type, printSettings);
		}


		throw new IllegalArgumentException("Don't know how to describe column of type %s".formatted(type));
	}

	public abstract void consume(@Nullable T value);

	public abstract ResultColumnStatistics describe();

	@Data
	@CPSBase
	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "chart")
	public abstract static class ResultColumnStatistics {
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
