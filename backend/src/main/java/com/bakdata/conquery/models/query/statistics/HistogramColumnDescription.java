package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import lombok.ToString;

@Getter
@CPSType(id = "HISTO", base = ColumnStatsCollector.ResultColumnStatistics.class)
@ToString(callSuper = true)
public class HistogramColumnDescription extends ColumnStatsCollector.ResultColumnStatistics {

	public static record Entry(String label, long value) {
	}

	;
	private final List<Entry> entries;

	private final Map<String, String> extras;

	public HistogramColumnDescription(String name, String label, String description, List<Entry> histogram, Map<String, String> extras, String type) {
		super(name, label, description, type);
		this.entries = histogram;
		this.extras = extras;
	}
}
