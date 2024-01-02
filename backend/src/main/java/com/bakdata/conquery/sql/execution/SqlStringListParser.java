package com.bakdata.conquery.sql.execution;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.util.Strings;

class SqlStringListParser {

	private static final Pattern UNIT_SEPARATOR = Pattern.compile(String.valueOf(ResultSetProcessor.UNIT_SEPARATOR));

	public static List<String> parse(String entry) {
		return Arrays.stream(UNIT_SEPARATOR.split(entry))
					 .filter(Strings::isNotEmpty)
					 .toList();
	}

}
