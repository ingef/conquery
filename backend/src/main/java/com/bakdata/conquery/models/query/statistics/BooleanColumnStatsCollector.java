package com.bakdata.conquery.models.query.statistics;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;

@Getter
public class BooleanColumnStatsCollector extends ColumnStatsCollector<Boolean> {

	private final StringColumnStatsCollector delegate;

	public BooleanColumnStatsCollector(String name, String label, String description, PrintSettings printSettings) {
		super(name, label, description, printSettings);
		delegate = new StringColumnStatsCollector(name, label, description, ResultType.StringT.INSTANCE, printSettings, Integer.MAX_VALUE);
	}

	@Override
	public void consume(Boolean value) {
		final String printed = value == null ? null : ResultType.BooleanT.INSTANCE.printNullable(getPrintSettings(), value);
		delegate.consume(printed);
	}

	@Override
	public ResultColumnStatistics describe() {
		return delegate.describe();
	}

}
