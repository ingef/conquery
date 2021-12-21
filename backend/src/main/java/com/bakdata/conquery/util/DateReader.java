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
 * Utility class for parsing multiple dateformats. Parsing is cached in two ways: First parsed values are cached. Second, the last used parser is cached since it's likely that it will be used again, we therefore try to use it first, then try all others.
 */
@Slf4j
public class DateReader {

	/**
	 * All available formats for parsing.
	 */
	@JsonIgnore
	private Set<DateTimeFormatter> dateFormats;

	@JsonIgnore
	private List<String> rangeStartEndSeperators;

	@JsonIgnore
	private List<LocaleConfig.ListFormat> dateSetLayouts;

	/**
	 * Last successfully parsed date format.
	 */
	@JsonIgnore
	private final ThreadLocal<DateTimeFormatter> lastDateFormat = new ThreadLocal<>();
	/**
	 * Last successfully parsed range format.
	 */
	@JsonIgnore
	private final ThreadLocal<String> lastRangeFormat = new ThreadLocal<>();

	/**
	 * Last successfully parsed dateset format.
	 */
	@JsonIgnore
	private final ThreadLocal<LocaleConfig.ListFormat> lastDateSetLayout = new ThreadLocal<>();

	@JsonIgnore
	private final LocalDate ERROR_DATE = LocalDate.MIN;

	/**
	 * Parsed values cache.
	 */
	@JsonIgnore
	private final LoadingCache<String, LocalDate> DATE_CACHE = CacheBuilder.newBuilder()
																		   .weakValues()
																		   .concurrencyLevel(10)
																		   .build(CacheLoader.from(this::tryParseDate));

	@JsonCreator
	public DateReader(Set<String> dateParsingFormats, List<String> rangeStartEndSeperators, List<LocaleConfig.ListFormat> dateSetLayouts) {
		this.dateFormats = dateParsingFormats.stream().map(DateTimeFormatter::ofPattern).collect(Collectors.toSet());
		this.rangeStartEndSeperators = rangeStartEndSeperators;
		this.dateSetLayouts = dateSetLayouts;
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

	public CDateRange parseToCDateRange(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		CDateRange result = null;

		final String lastSep = lastRangeFormat.get();
		if (lastSep != null) {
			try {
				return parseToCDateRange(value, lastSep);
			}
			catch (ParsingException e) {
				log.info("Parsing with last used config failed for date range: {}", value, e);
			}
		}

		for (String sep : rangeStartEndSeperators) {
			try {
				result = parseToCDateRange(value, sep);
				lastRangeFormat.set(sep);
			}
			catch (ParsingException e) {
				log.info("Parsing failed for date range: {}", value, e);
				continue;
			}
			break;
		}
		if (result != null) {
			return result;
		}
		throw new ParsingException("None of the configured formats allowed to parse the date range: " + value);
	}

	private CDateRange parseToCDateRange(String value, String sep) {
		log.info("Parsing `{}` using Sep `{}`", value, sep);

		String[] parts = StringUtils.split(value, sep);

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

		throw ParsingException.of(value, "daterange");
	}

	public CDateSet parseToCDateSet(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		CDateSet result = null;

		final LocaleConfig.ListFormat lastDateSet = lastDateSetLayout.get();
		if (lastDateSet != null) {
			try {
				log.info("Parsing CDateSet using {}", lastDateSet);
				return lastDateSet.parse(value, this);
			}
			catch (ParsingException e) {
				log.trace("Parsing with last used config failed for date set: {}", value, e);
			}
		}

		for (LocaleConfig.ListFormat sep : dateSetLayouts) {
			try {
				result = sep.parse(value, this);
			}
			catch (ParsingException e) {
				log.trace("Parsing failed for date set '{}' with pattern '{}'", value, sep, e);
				continue;
			}
			lastDateSetLayout.set(sep);
			break;
		}
		if (result != null) {
			return result;
		}
		throw new ParsingException("Non of the configured formats allowed to parse the date set: " + value);
	}


	/**
	 * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
	 * <p>
	 * Method is private as it is only directly accessed via the Cache.
	 */
	private LocalDate tryParseDate(String value) {

		final DateTimeFormatter formatter = lastDateFormat.get();

		if (formatter != null) {
			try {
				return LocalDate.parse(value, formatter);
			}
			catch (DateTimeParseException e) {
				//intentionally left blank
			}
		}

		for (DateTimeFormatter format : dateFormats) {
			if (formatter != format) {
				try {
					LocalDate res = LocalDate.parse(value, format);
					lastDateFormat.set(format);
					return res;
				}
				catch (DateTimeParseException e) {
					//intentionally left blank
				}
			}
		}
		return ERROR_DATE;
	}

	private DateTimeFormatter createFormatter(String pattern) {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
	}
}
