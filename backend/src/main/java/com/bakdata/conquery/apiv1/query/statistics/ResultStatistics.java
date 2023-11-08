package com.bakdata.conquery.apiv1.query.statistics;

import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.Data;

@Data
public class ResultStatistics {
	private final int entities;
	private final int total;
	private final List<ColumnStatsCollector.ResultColumnStatistics> statistics;
	private final CDateRange dateRange;
}
