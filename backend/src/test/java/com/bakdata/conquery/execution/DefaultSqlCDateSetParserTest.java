package com.bakdata.conquery.execution;

import java.util.stream.Stream;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.sql.execution.DefaultSqlCDateSetParser;
import com.bakdata.conquery.util.DateReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultSqlCDateSetParserTest {

	private static final DefaultSqlCDateSetParser DATE_SET_PARSER = new DefaultSqlCDateSetParser();

	private static final DateReader DATE_READER = new LocaleConfig().getDateReader();


	public static Stream<Arguments> testToEpochDayRangeListProvider() {
		return Stream.of(
				Arguments.of("{}", "{}", "Empty datemultirange"),
				Arguments.of("{[-∞,∞]}", "{/}", "Infinity datemultirange"),
				Arguments.of("{[2012-01-01,2013-01-01)}", "{2012-01-01/2012-12-31}", "datemultirange with 1 daterange"),
				Arguments.of("{[-∞,2013-01-01),[2015-01-01,∞]}", "{/2012-12-31,2015-01-01/}", "datemultirange with multiple ranges and infinity start and end value"),
				Arguments.of("{[2014-01-01,2015-01-01),[2015-06-01,2016-01-01),[2017-01-01,2018-01-01)}",
							 "{2014-01-01/2014-12-31,2015-06-01/2015-12-31,2017-01-01/2017-12-31}",
							 "datemultirange with multiple ranges"
				)
		);
	}

	@ParameterizedTest
	@MethodSource("testToEpochDayRangeListProvider")
	public void testToEpochDayRangeList(String input, String expectedRaw, String message) {
		CDateSet actual = CDateSet.create(DATE_SET_PARSER.toEpochDayRangeList(input).stream()
														 .map(CDateRange::fromList)
														 .toList());

		CDateSet expected = DATE_READER.parseToCDateSet(expectedRaw);

		Assertions.assertEquals(expected, actual, message);
	}

}
