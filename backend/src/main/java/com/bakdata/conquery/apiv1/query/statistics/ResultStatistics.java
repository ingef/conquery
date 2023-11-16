package com.bakdata.conquery.apiv1.query.statistics;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.common.Range;

public record ResultStatistics(int entities, int total, List<ColumnStatsCollector.ResultColumnStatistics> statistics, Range<LocalDate> dateRange) {
}
