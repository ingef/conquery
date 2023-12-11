package com.bakdata.conquery.sql.execution;

import java.util.List;

public interface SqlCDateSetParser {

	List<List<Integer>> toEpochDayRangeList(String multiDateRange);

	List<Integer> toEpochDayRange(String daterange);

}
