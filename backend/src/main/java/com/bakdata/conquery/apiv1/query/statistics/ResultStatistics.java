package com.bakdata.conquery.apiv1.query.statistics;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.common.Range;
import lombok.Data;

@Data
public class ResultStatistics {
	private final int entities;
	private final int total;
	private final List<ColumnStatsCollector.ResultColumnStatistics> statistics;
	private final Range<LocalDate> dateRange;
}
