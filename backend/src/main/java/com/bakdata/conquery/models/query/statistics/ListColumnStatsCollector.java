package com.bakdata.conquery.models.query.statistics;

import java.util.Collection;

import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;


@ToString
@Getter
public class ListColumnStatsCollector<T> extends ColumnStatsCollector {

	private final ColumnStatsCollector underlying;

	public ListColumnStatsCollector(String name, String label, String description, ColumnStatsCollector underlying, PrintSettings printSettings) {
		super(name, label, description, printSettings);
		this.underlying = underlying;
	}

	@Override
	public void consume(@Nullable Object values) {
		if(values == null){
			return;
		}

		for (Object value : (Collection) values) {
			underlying.consume(value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		return underlying.describe();
	}
}
