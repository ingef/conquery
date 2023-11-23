package com.bakdata.conquery.models.query.statistics;

import java.util.Collection;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;


@ToString
@Getter
public class ListColumnStatsCollector<T> extends ColumnStatsCollector<Collection<T>>{

	private final ColumnStatsCollector<T> underlying;

	public ListColumnStatsCollector(String name, String label, String description, ResultType type, ColumnStatsCollector<T> underlying, PrintSettings printSettings) {
		super(name, label, description, type, printSettings);
		this.underlying = underlying;
	}

	@Override
	public void consume(@Nullable Collection<T> values) {
		if(values == null){
			return;
		}

		for (T value : values) {
			underlying.consume(value);
		}
	}

	@Override
	public ResultColumnStatistics describe() {
		return underlying.describe();
	}
}
