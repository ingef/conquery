package com.bakdata.conquery.sql.execution;

import java.io.StringReader;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class PostgreSqlCDateSetParser implements SqlCDateSetParser {
	//TODO clean this up and use provided dateReader instead of Date.valueOf
	//TODO can probably be unified with Hana implementation by supplying separators as fields

	@Override
	public List<List<Integer>> toEpochDayRangeList(String multiDateRange) {

		if (Strings.isNullOrEmpty(multiDateRange) || "empty".equals(multiDateRange)) {
			return Collections.emptyList();
		}

		return parse(multiDateRange);
	}

	@Override
	public List<Integer> toEpochDayRange(String daterange) {
		if (daterange == null) {
			return Collections.emptyList();
		}

		String[] dates = daterange.split(",");

		Preconditions.checkArgument(dates.length == 2, "Dateranges must have a start and end.");

		// the dateranges have always an included start date marked by a [
		String startDateExpression = dates[0].substring(1);
		int startDate;

		if (startDateExpression.equals("-infinity")) {
			startDate = CDateRange.NEGATIVE_INFINITY;
		}
		else {
			startDate = CDate.ofLocalDate(Date.valueOf(startDateExpression).toLocalDate());
		}

		String endDateExpression = dates[1].substring(0, dates[1].length() - 1);
		int endDate;

		if (endDateExpression.equals("infinity")) {
			endDate = CDateRange.POSITIVE_INFINITY;
		}
		else {
			LocalDate dateValue = Date.valueOf(endDateExpression).toLocalDate();
			endDate = CDate.ofLocalDate(dateValue);
			if (dates[1].endsWith(")")) {
				endDate -= 1;
			}
		}

		return List.of(startDate, endDate);
	}

	private List<List<Integer>> parse(String multiDateRange) {
		StringReader reader = new StringReader(multiDateRange);

		Scanner scanner = new Scanner(reader)
				// ) and ] are handled manually to determine real end-date
				.useDelimiter(Pattern.compile("[,{}\\[(]+"));

		ArrayList<List<Integer>> out = new ArrayList<>();

		while (scanner.hasNext()) {
			String start = scanner.next();
			String end = scanner.next();

			int startCDate;
			if (start.equals("-infinity")) {
				startCDate = Integer.MIN_VALUE;
			}
			else {
				LocalDate startDate = Date.valueOf(start).toLocalDate();
				startCDate = CDate.ofLocalDate(startDate);
			}

			int endCDate;
			if (end.startsWith("infinity")) {
				endCDate = Integer.MAX_VALUE;
			}
			else {
				LocalDate endDate = Date.valueOf(end.substring(0, end.length() - 1)).toLocalDate();
				endCDate = CDate.ofLocalDate(endDate);

				if (end.endsWith(")")) {
					endCDate -= 1;
				}
			}
			out.add(List.of(startCDate, endCDate));
		}


		return out;
	}

}
