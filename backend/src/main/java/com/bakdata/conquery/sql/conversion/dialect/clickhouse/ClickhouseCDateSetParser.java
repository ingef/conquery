package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.NotImplementedException;

//TODO Clickhouse supports Arrays and tuples, we should use those natively instead
public class ClickhouseCDateSetParser implements SqlCDateSetParser {

	public static final String DATE_SEPARATOR = ",";
	public static final String INCLUDED_START_CHAR = "'[";
	public static final String EXCLUDED_END_CHAR = ")'";

	@Override
	public List<List<Integer>> toEpochDayRangeList(String multiDateRange) {
		throw new NotImplementedException();
	}

	public List<List<Integer>> toEpochDayRangeList(Object[] raw) {
		List<List<Integer>> out = new ArrayList<>();

		for (Object rawObj : raw) {
			Object[] rawTuple = (Object[]) rawObj;
			out.add(List.of((Integer) rawTuple[0], (Integer) rawTuple[1] - 1));
		}

		return out;
	}

	@Override
	public List<Integer> toEpochDayRange(String daterange) {

		if (daterange == null) {
			return Collections.emptyList();
		}

		String[] dates = daterange.split(DATE_SEPARATOR);
		Preconditions.checkArgument(dates.length == 2, "Dateranges must have a start and end.");

		// the dateranges have always an included start date marked by a [
		String startDateExpression = dates[0];
		int startDate;
		if (startDateExpression.equals(ClickhouseFunctionProvider.MIN_DATE_VALUE)) {
			startDate = CDateRange.NEGATIVE_INFINITY;
		}
		else {
			LocalDate dateValue = Date.valueOf(startDateExpression).toLocalDate();
			startDate = CDate.ofLocalDate(dateValue);
		}

		String endDateExpression = dates[1];
		int endDate;
		if (endDateExpression.equals(ClickhouseFunctionProvider.MAX_DATE_VALUE)) {
			endDate = CDateRange.POSITIVE_INFINITY;
		}
		else {
			LocalDate dateValue = Date.valueOf(endDateExpression).toLocalDate();
			endDate = CDate.ofLocalDate(dateValue);
		}

		return List.of(startDate, endDate);
	}

}
