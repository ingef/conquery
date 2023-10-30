package com.bakdata.conquery.sql.execution;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;

public class DefaultNotationParser implements CDateSetParser {

	public static final String EMPTY_RANGE_BRACES = "{}";
	public static final String DATE_SEPARATOR = ",";
	public static final char INCLUDED_START_CHAR = '[';
	public static final char EXCLUDED_END_CHAR = ')';

	public CDateSet fromString(String multiDateRange) {

		if (multiDateRange.equals(EMPTY_RANGE_BRACES)) {
			return CDateSet.createEmpty();
		}

		List<CDateRange> cDateRanges = new ArrayList<>();
		List<String> dateranges = parse(multiDateRange);

		for (String daterange : dateranges) {

			String[] dates = daterange.split(DATE_SEPARATOR);

			// postgres dates have allways an included start date marked by a [
			String startDateExpression = dates[0];
			LocalDate startDate;
			if (startDateExpression.contains(SqlFunctionProvider.MINUS_INFINITY_SIGN)) {
				startDate = LocalDate.ofEpochDay(CDateRange.NEGATIVE_INFINITY);
			}
			else {
				startDate = Date.valueOf(startDateExpression.substring(1)).toLocalDate();
			}

			String endDateExpression = dates[1];
			LocalDate endDate;
			if (endDateExpression.contains(SqlFunctionProvider.INFINITY_SIGN)) {
				endDate = LocalDate.ofEpochDay(CDateRange.POSITIVE_INFINITY);
			}
			else {
				endDate = Date.valueOf(endDateExpression.substring(0, endDateExpression.length() - 1)).toLocalDate();
				endDate = endDateExpression.charAt(endDateExpression.length() - 1) == EXCLUDED_END_CHAR
						  ? endDate.minusDays(1)
						  : endDate;
			}

			cDateRanges.add(CDateRange.of(startDate, endDate));
		}

		return CDateSet.create(cDateRanges);
	}

	/**
	 * Captures any postgres-like datemultirange notation.
	 *
	 * <p>
	 * From {@code {[2012-01-01,2013-01-01),[2015-01-01,2016-01-01]}}, it would capture:
	 * <ul>
	 *   <li><b>[2012-01-01,2013-01-01)</b></li>
	 *   <li><b>[2015-01-01,2016-01-01)</b></li>
	 * </ul>
	 */
	private List<String> parse(String multiDateRange) {

		List<String> dateranges = new ArrayList<>();

		// strip of curly braces
		String current = multiDateRange.substring(1, multiDateRange.length() - 1);

		int daterangeStart = current.indexOf(INCLUDED_START_CHAR);
		while (daterangeStart != -1) {
			int nextDaterangeStart = current.indexOf(INCLUDED_START_CHAR, 1);
			if (nextDaterangeStart == -1) {
				dateranges.add(current);
			}
			else {
				// if there is a following daterange, we also strip of the comma
				dateranges.add(current.substring(0, nextDaterangeStart - 1));
				current = current.substring(nextDaterangeStart);
			}
			daterangeStart = nextDaterangeStart;
		}

		return dateranges;
	}

}
