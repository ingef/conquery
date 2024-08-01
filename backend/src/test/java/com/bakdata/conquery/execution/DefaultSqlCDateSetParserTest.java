package com.bakdata.conquery.execution;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.execution.DefaultSqlCDateSetParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultSqlCDateSetParserTest {

	private static final DefaultSqlCDateSetParser parser = new DefaultSqlCDateSetParser();
	private static final ConqueryConfig CONFIG = new ConqueryConfig();
	private static final PrintSettings PLAIN = new PrintSettings(false, Locale.ENGLISH, null, CONFIG, null);

	@ParameterizedTest
	@MethodSource("testToEpochDayRangeListProvider")
	public void testToEpochDayRangeList(String input, String expected, String message) {
		List<List<Integer>> epochDayRangeList = parser.toEpochDayRangeList(input);
		String actual = new ResultType.ListT(ResultType.DateRangeT.getINSTANCE()).printNullable(PLAIN, epochDayRangeList);
		Assertions.assertEquals(expected, actual, message);
	}

	public static Stream<Arguments> testToEpochDayRangeListProvider() {
		return Stream.of(
				Arguments.of("{}", "{}", "Empty datemultirange"),
				Arguments.of("{[-∞,∞]}", "{-∞/+∞}", "Infinity datemultirange"),
				Arguments.of("{[2012-01-01,2013-01-01)}", "{2012-01-01/2012-12-31}", "datemultirange with 1 daterange"),
				Arguments.of("{[-∞,2013-01-01),[2015-01-01,∞]}", "{-∞/2012-12-31,2015-01-01/+∞}", "datemultirange with multiple ranges and infinity start and end value"),
				Arguments.of("{[2014-01-01,2015-01-01),[2015-06-01,2016-01-01),[2017-01-01,2018-01-01)}", "{2014-01-01/2014-12-31,2015-06-01/2015-12-31,2017-01-01/2017-12-31}", "datemultirange with multiple ranges")
		);
	}

}
