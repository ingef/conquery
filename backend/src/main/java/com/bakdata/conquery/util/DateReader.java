package com.bakdata.conquery.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
	private List<LocaleConfig.DateSetLayout> dateSetLayouts;

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
	private final ThreadLocal<Pattern> lastDateSetLayout = new ThreadLocal<>();

	@JsonIgnore
	private final LocalDate ERROR_DATE = LocalDate.MIN;

	/**
	 * Parsed values cache.
	 */
	@JsonIgnore
	private final LoadingCache<String, LocalDate> DATE_CACHE = CacheBuilder.newBuilder()
																		   .weakValues()
																		   .concurrencyLevel(10)
																		   .build(CacheLoader.from(this::tryParse));

	@JsonCreator
	public DateReader(Set<String> dateParsingFormats, List<String> rangeStartEndSeperators, List<LocaleConfig.DateSetLayout> dateSetLayouts) {
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
			try{
				return parseToCDateRange(value,lastSep);
			} catch (ParsingException e) {
				log.trace("Parsing with last used config failed for date range: " + value, e);
			}
		}

		for(String sep : rangeStartEndSeperators) {
			try {
				result = parseToCDateRange(value,sep);
			} catch (ParsingException e) {
				log.trace("Parsing failed for date range: " + value, e);
				continue;
			}
			lastRangeFormat.set(sep);
		}
		if (result != null) {
			return result;
		}
		throw new ParsingException("Non of the configured formats allowed to parse the date range: " + value);
	}

	private CDateRange parseToCDateRange(String value, String sep) {
		String[] parts = StringUtils.split(value, sep);
		if (parts.length != 2) {
			throw ParsingException.of(value, "daterange");
		}

		return CDateRange.of(
				parseToLocalDate(parts[0]),
				parseToLocalDate(parts[1])
		);
	}

	public CDateSet parseToCDateSet(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		CDateSet result = null;

		final Pattern lastDateSet = lastDateSetLayout.get();
		if (lastDateSet != null) {
			try{
				return parseToCDateSet(value,lastDateSet);
			} catch (ParsingException e) {
				log.trace("Parsing with last used config failed for date set: " + value, e);
			}
		}

		for(LocaleConfig.DateSetLayout sep : dateSetLayouts) {
			Pattern regexPattern = generateDateSetPattern(sep.getSetBegin(), sep.getRangeSep(), sep.getSetEnd());
			try {
				result = parseToCDateSet(value,regexPattern);
			} catch (ParsingException e) {
				log.trace("Parsing failed for date set: " + value, e);
				continue;
			}
			lastDateSetLayout.set(regexPattern);
		}
		if (result != null) {
			return result;
		}
		throw new ParsingException("Non of the configured formats allowed to parse the date set: " + value);
	}


	public CDateSet parseToCDateSet(String value, Pattern pattern) {
		List<CDateRange> ranges = pattern.matcher(value)
				.results()
				.map(mr -> parseToCDateRange(mr.group(2)))
				.collect(Collectors.toList());
		return CDateSet.create(ranges);

	}

	private static Pattern generateDateSetPattern(@NonNull String setBegin, @NonNull String rangeSep, @NonNull String setEnd) {
		assert(setBegin.length() < 2);
		assert(rangeSep.length() == 1);
		assert(setEnd.length() < 2);

		return Pattern.compile(String.format("(%1$s|%2$s\\s*)([^%1$s%2$s%3$s]*)", Pattern.quote(setBegin), Pattern.quote(rangeSep), Pattern.quote(setEnd)));
	}

	/**
	 * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
	 * <p>
	 * Method is private as it is only directly accessed via the Cache.
	 */
	private LocalDate tryParse(String value) {

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
