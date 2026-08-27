package com.bakdata.conquery.sql.execution;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.util.DateReader;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DefaultCDateSetParser implements SqlCDateSetParser {
	private final DateReader dateReader;

	private final String dateSetSep;
	private final String dateRangeSep;
	private final String dateRangeMinValue;
	private final String dateRangeMaxValue;

	private final String dateRangeEndExclusive;
	private final String dateRangeEndInclusive;
	private final String dateRangeBeginInclusive;

	private final String dateSetStart;
	private final String dateSetEnd;
	private final String emptyDateSet;

	//TODO clean this up and use provided dateReader instead of Date.valueOf
	//TODO can probably be unified with Hana implementation by supplying separators as fields

	@Override
	public List<List<Integer>> toEpochDayRangeList(String multiDateRange) {

		if (Strings.isNullOrEmpty(multiDateRange) || emptyDateSet.equals(multiDateRange)) {
			return Collections.emptyList();
		}

		StringReader reader = new StringReader(multiDateRange);

		Scanner scanner = new Scanner(reader)
			// ) and ] are handled manually to determine real end-date
			.useDelimiter(
				Pattern.compile(
					"[%s]+".formatted(
						Stream.of(dateSetSep, dateRangeSep, dateSetStart, dateSetEnd, dateRangeBeginInclusive)
							.map(
								Pattern::quote)
							.collect(Collectors.joining()))));


		ArrayList<List<Integer>> out = new ArrayList<>();

		while (scanner.hasNext()) {
			String start = scanner.next();
			String end = scanner.next();

			out.add(tupleFromStrings(start, end));
		}


		return out;
	}

	@Override
	public List<Integer> toEpochDayRange(String daterange) {
		if (daterange == null) {
			return Collections.emptyList();
		}

		String[] dates = daterange.split(dateRangeSep);

		Preconditions.checkArgument(dates.length == 2, "Dateranges must have a start and end.");

		return tupleFromStrings(dates[0].substring(1), dates[1]);
	}

	private List<Integer> tupleFromStrings(String begin, String end) {
		// the dateranges have always an included start date marked by a [
		int startDate;

		if (begin.equals(dateRangeMinValue)) {
			startDate = CDateRange.NEGATIVE_INFINITY;
		} else {
			startDate = CDate.ofLocalDate(dateReader.parseToLocalDate(begin));
		}

		String endDateExpression = end.substring(0, end.length() - 1);
		int endDate;

		if (endDateExpression.equals(dateRangeMaxValue)) {
			endDate = CDateRange.POSITIVE_INFINITY;
		} else {
			LocalDate dateValue = dateReader.parseToLocalDate(endDateExpression);
			endDate = CDate.ofLocalDate(dateValue);
			if (end.endsWith(dateRangeEndExclusive)) {
				endDate -= 1;
			}
		}

		return List.of(startDate, endDate);
	}


}
