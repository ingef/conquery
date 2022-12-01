package com.bakdata.conquery.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for parsing multiple date-formats.
 * <p>
 * We cache successfully parsed dates, hoping to achieve a speedup that way.
 * <p>
 * We also assume that date-formats do not change over the course of parsing and use the last successfully parsed format as candidate for parsing other values.
 */
@Slf4j
public class DateReader {

	/**
	 * Index of the last successfully parsed date format in dateFormats.
	 */
	@JsonIgnore
	private final ThreadLocal<Integer> lastDateFormatIndex = ThreadLocal.withInitial(() -> 0);
	/**
	 * Index of the last successfully parsed date range format in rangeStartEndSeperators.
	 */
	@JsonIgnore
	private final ThreadLocal<Integer> lastRangeFormatIndex = ThreadLocal.withInitial(() -> 0);
	/**
	 * Index of the last successfully parsed dateset format in dateSetLayouts
	 */
	@JsonIgnore
	private final ThreadLocal<Integer> lastDateSetLayoutIndex = ThreadLocal.withInitial(() -> 0);
	@JsonIgnore
	private final LocalDate ERROR_DATE = LocalDate.MIN;
	/**
	 * All available formats for parsing.
	 */
	@JsonIgnore
	private List<DateTimeFormatter> dateFormats;
	/**
	 * Parsed values cache.
	 */
	@JsonIgnore
	private final LoadingCache<String, LocalDate> DATE_CACHE = CacheBuilder.newBuilder()
																		   .weakValues()
																		   .concurrencyLevel(10)
																		   .build(CacheLoader.from(this::tryParseDate));
	@JsonIgnore
	private List<String> rangeStartEndSeperators;
	@JsonIgnore
	private List<LocaleConfig.ListFormat> dateSetLayouts;

	@JsonCreator
	public DateReader(Set<String> dateParsingFormats, List<String> rangeStartEndSeperators, List<LocaleConfig.ListFormat> dateSetLayouts) {
		this.dateFormats = dateParsingFormats.stream().map(DateTimeFormatter::ofPattern).collect(Collectors.toList());
		this.rangeStartEndSeperators = rangeStartEndSeperators;
		this.dateSetLayouts = dateSetLayouts;
	}

	/**
	 * Try and parse value to {@link CDateRange} using all available rangeFormats, starting at the last known successful one.
	 */
	public CDateRange parseToCDateRange(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		final int root = lastRangeFormatIndex.get();

		for (int offset = 0; offset < rangeStartEndSeperators.size(); offset++) {
			final int index = (root + offset) % rangeStartEndSeperators.size();

			String sep = rangeStartEndSeperators.get(index);
			try {
				CDateRange result = parseToCDateRange(value, sep);
				lastRangeFormatIndex.set(index);
				return result;
			}
			catch (ParsingException e) {
				log.trace("Parsing failed for date range `{}` using `{}`", value, sep, e);
			}
		}

		throw new ParsingException("None of the configured formats allowed to parse the date range: " + value);
	}

	/**
	 * Try and parse value to {@link CDateRange} using the supplied separator.
	 */
	private CDateRange parseToCDateRange(String value, String sep) {

		// Shorthand formats for open ranges without resorting to two-column formats
		if (value.startsWith(sep)) {
			return CDateRange.atMost(parseToLocalDate(value.substring(sep.length())));
		}

		if (value.endsWith(sep)) {
			return CDateRange.atLeast(parseToLocalDate(value.substring(0, value.length() - sep.length())));
		}

		if(!value.contains(sep)){
			throw ParsingException.of(value, String.format("DateRange: Did not contain sep `%s`", sep));
		}

		final String[] parts = StringUtils.split(value, sep);


		if (parts.length == 1) {
			// If it looks like a single date, try to parse it at one
			return CDateRange.exactly(parseToLocalDate(parts[0]));
		}

		if (parts.length == 2) {
			return CDateRange.of(
					parseToLocalDate(parts[0]),
					parseToLocalDate(parts[1])
			);
		}

		throw ParsingException.of(value, String.format("DateRange: Unexpected length of Parts (%d) for `%s` using sep=`%s`", parts.length, value, sep));
	}

	/**
	 * Try parsing the String value to a LocalDate.
	 */
	public LocalDate parseToLocalDate(String value) throws ParsingException {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		final LocalDate out = DATE_CACHE.getUnchecked(value);

		if (out.equals(ERROR_DATE)) {
			throw new ParsingException(String.format("Failed to parse `%s` as LocalDate.", value));
		}

		return out;
	}

	/**
	 * Try and parse value to CDateSet using all available layouts, but starting at the last known successful one.
	 */
	public CDateSet parseToCDateSet(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		final int root = lastDateSetLayoutIndex.get();

		for (int offset = 0; offset < dateSetLayouts.size(); offset++) {
			final int index = (root + offset) % dateSetLayouts.size();

			final LocaleConfig.ListFormat sep = dateSetLayouts.get(index);

			try {
				final CDateSet result = sep.parse(value, this);
				lastDateSetLayoutIndex.set(index);
				return result;
			}
			catch (ParsingException e) {
				log.trace("Parsing failed for date set '{}' with pattern '{}'", value, sep, e);
			}
		}

		throw new ParsingException("None of the configured formats to parse the date set: " + value);
	}


	/**
	 * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
	 * <p>
	 * Method is private as it is only directly accessed via the Cache.
	 */
	private LocalDate tryParseDate(String value) {

		final int root = lastDateFormatIndex.get();

		for (int offset = 0; offset < dateFormats.size(); offset++) {
			final int index = (root + offset) % dateFormats.size();
			final DateTimeFormatter format = dateFormats.get(index);

			try {
				final LocalDate res = LocalDate.parse(value, format);

				lastDateFormatIndex.set(index);
				return res;
			}
			catch (DateTimeParseException e) {
				log.trace("Failed to parse date `{}` using `{}`", value, format, e);
			}
		}
		// We return ERROR_DATE here, so faulty values are also cached, the exception is thrown at the usage of the cache.
		return ERROR_DATE;
	}

	private DateTimeFormatter createFormatter(String pattern) {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
	}
}
