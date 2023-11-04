package com.bakdata.conquery.sql.execution;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultNotationParserTest {

	private static final DefaultNotationParser parser = new DefaultNotationParser();

	@ParameterizedTest
	@MethodSource("multiDaterangeStringProvider")
	public void testFromString(String input, String expected, String message) {
		Assertions.assertEquals(expected, parser.fromString(input).toString(), message);
	}

	public static Stream<Arguments> multiDaterangeStringProvider() {
		return Stream.of(
				Arguments.of("{}", "{}", "Empty datemultirange"),
				Arguments.of("{[-∞,∞]}", "{-∞/+∞}", "Infinity datemultirange"),
				Arguments.of("{[2012-01-01,2013-01-01)}", "{2012-01-01/2012-12-31}", "datemultirange with 1 daterange"),
				Arguments.of("{[-∞,2013-01-01),[2015-01-01,∞]}", "{-∞/2012-12-31, 2015-01-01/+∞}", "datemultirange with multiple ranges and infinity start and end value"),
				Arguments.of("{[2014-01-01,2015-01-01),[2015-06-01,2016-01-01),[2017-01-01,2018-01-01)}", "{2014-01-01/2014-12-31, 2015-06-01/2015-12-31, 2017-01-01/2017-12-31}", "datemultirange with multiple ranges")
		);
	}

}
