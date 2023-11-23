package com.bakdata.conquery.models.query.statistics;

import java.util.concurrent.atomic.AtomicLong;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;

@Getter
public class BooleanColumnStatsCollector extends ColumnStatsCollector<Boolean> {

	private final AtomicLong trues = new AtomicLong();
	private final AtomicLong falses = new AtomicLong();
	private final AtomicLong nulls = new AtomicLong(0);

	public BooleanColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings) {
		super(name, label, description, type, printSettings);
	}

	@Override
	public void consume(Boolean value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		if (value){
			trues.incrementAndGet();
		}
		else {
			falses.incrementAndGet();
		}
	}

	@Override
	public ResultColumnStatistics describe() {

		return new ColumnDescription(getName(), getLabel(), getDescription(), getType().toString(), trues.get(), falses.get(), nulls.get(), nulls.get() + trues.get() + falses.get());
	}

	@Getter
	@CPSType(id = "BOOLEAN", base = ResultColumnStatistics.class)
	@ToString(callSuper = true)
	public static class ColumnDescription extends ResultColumnStatistics {
		private final long trues;
		private final long falses;
		private final long nulls;
		private final long total;

		public ColumnDescription(String name, String label, String description, String type, long trues, long falses, long nulls, long total) {
			super(name, label, description, type);
			this.trues = trues;
			this.falses = falses;
			this.nulls = nulls;
			this.total = total;
		}
	}
}
