package com.bakdata.conquery.sql.execution;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlFunctionProvider;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class HanaSqlCDateSetParser implements SqlCDateSetParser {

	@Override
	public List<List<Integer>> toEpochDayRangeList(String multiDateRange) {

		if (Strings.isNullOrEmpty(multiDateRange)) {
			return Collections.emptyList();
		}

		List<List<Integer>> result = new ArrayList<>();

		List<String> dateranges = parse(multiDateRange);

		for (String daterange : dateranges) {
			result.add(toEpochDayRange(daterange));
		}

		return result;
	}

	@Override
	public List<Integer> toEpochDayRange(String daterange) {
		if (daterange == null) {
			return Collections.emptyList();
		}

		String[] dates = daterange.split(HanaSqlFunctionProvider.DATERANGE_SEPARATOR);

		Preconditions.checkArgument(dates.length == 2, "Dateranges must have a start and end.");

		// the dateranges have always an included start date marked by a [
		String startDateExpression = dates[0];
		int startDate;

		if (startDateExpression.equals(HanaSqlFunctionProvider.MIN_DATE_VALUE)) {
			startDate = CDateRange.NEGATIVE_INFINITY;
		}
		else {
			startDate = CDate.ofLocalDate(Date.valueOf(startDateExpression).toLocalDate());
		}

		String endDateExpression = dates[1];
		int endDate;

		if (endDateExpression.equals(HanaSqlFunctionProvider.MAX_DATE_VALUE)) {
			endDate = CDateRange.POSITIVE_INFINITY;
		}
		else {
			LocalDate dateValue = Date.valueOf(endDateExpression).toLocalDate();
			endDate = CDate.ofLocalDate(dateValue) - 1;
		}

		return List.of(startDate, endDate);
	}

	private List<String> parse(String multiDateRange) {
		String[] split = multiDateRange.split(String.valueOf(ResultSetProcessor.UNIT_SEPARATOR));
		return List.of(split);
	}

}
