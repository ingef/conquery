package com.bakdata.conquery.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing multiple dateformats. Parsing is cached in two ways: First parsed values are cached. Second, the last used parser is cached since it's likely that it will be used again, we therefore try to use it first, then try all others.
 */
@UtilityClass
@Slf4j
public class DateFormats {


	/**
	 * All available formats for parsing.
	 */
	private static Set<DateTimeFormatter> formats;

	/**
	 * Last successfully parsed dateformat.
	 */
	private static ThreadLocal<DateTimeFormatter> lastFormat = new ThreadLocal<>();

	private static final LocalDate ERROR_DATE = LocalDate.MIN;

	/**
	 * Parsed values cache.
	 */
	private static final LoadingCache<String, LocalDate> DATE_CACHE = CacheBuilder.newBuilder()
																				  .weakKeys().weakValues()
																				  .concurrencyLevel(10)
																				  .initialCapacity(64000)
																				  .build(CacheLoader.from(DateFormats::tryParse));

	/**
	 * Try parsing the String value to a LocalDate.
	 */
	public static LocalDate parseToLocalDate(String value) throws ParsingException {
		if(Strings.isNullOrEmpty(value)) {
			return null;
		}

		final LocalDate out = DATE_CACHE.getUnchecked(value);

		if(out.equals(ERROR_DATE)) {
			throw new IllegalArgumentException(String.format("Failed to parse `%s` as LocalDate.", value));
		}

		return out;
	}

	/**
	 * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
	 *
	 * Method is private as it is only directly accessed via the Cache.
	 */
	private static LocalDate tryParse(String value) {

		if (formats == null) {
			initializeFormatters();
		}

		final DateTimeFormatter formatter = lastFormat.get();

		if (formatter != null) {
			try {
				return LocalDate.parse(value, formatter);
			} catch (DateTimeParseException e) {
				//intentionally left blank
			}
		}

		for (DateTimeFormatter format : formats) {
			if (formatter != format) {
				try {
					LocalDate res = LocalDate.parse(value, format);
					lastFormat.set(format);
					return res;
				} catch (DateTimeParseException e) {
					//intentionally left blank
				}
			}
		}
		return ERROR_DATE;
	}

	private static DateTimeFormatter createFormatter(String pattern) {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
	}

	/**
	 * Lazy-initialize all formatters. Load additional formatters via ConqueryConfig.
	 */
	private static void initializeFormatters() {
		final HashSet<DateTimeFormatter> formatters = new HashSet<>();

		formatters.add(createFormatter("yyyy-MM-dd"));
		formatters.add(createFormatter("ddMMMyyyy"));
		formatters.add(createFormatter("yyyyMMdd"));
		for (String p : ConqueryConfig.getInstance().getAdditionalFormats()) {
			formatters.add(createFormatter(p));
		}

		formats = Collections.unmodifiableSet(formatters);
	}
}
