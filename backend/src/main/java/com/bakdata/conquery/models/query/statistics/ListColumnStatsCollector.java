package com.bakdata.conquery.models.query.statistics;

import java.util.Collection;

import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;


@ToString
@Getter
public class ListColumnStatsCollector extends ColumnStatsCollector {

	private final ColumnStatsCollector underlying;

	public ListColumnStatsCollector(ColumnStatsCollector underlying, PrintSettings printSettings) {
		super(null, null, null, printSettings);
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
