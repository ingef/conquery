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
			try{
				return parseToCDateRange(value,lastSep);
			} catch (ParsingException e) {
				log.info("Parsing with last used config failed for date range: {}", value, e);
			}
		}

		for(String sep : rangeStartEndSeperators) {
			try {
				result = parseToCDateRange(value,sep);
				lastRangeFormat.set(sep);
			} catch (ParsingException e) {
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

		final Pattern lastDateSet = lastDateSetLayout.get();
		if (lastDateSet != null) {
			try{
				log.info("Parsing CDateSet using {}", lastDateSet);
				return parseToCDateSet(value,lastDateSet);
			} catch (ParsingException e) {
				log.trace("Parsing with last used config failed for date set: " + value, e);
			}
		}

		for (LocaleConfig.ListFormat sep : dateSetLayouts) {
			Pattern regexPattern = generateDateSetPattern(sep.getStart(), sep.getSeparator(), sep.getEnd());
			try {
				result = parseToCDateSet(value, regexPattern);
			}
			catch (ParsingException e) {
				log.trace("Parsing failed for date set '" + value + "' with pattern '" + regexPattern + "'", e);
				continue;
			}
			lastDateSetLayout.set(regexPattern);
			break;
		}
		if (result != null) {
			return result;
		}
		throw new ParsingException("Non of the configured formats allowed to parse the date set: " + value);
	}


	public CDateSet parseToCDateSet(String value, Pattern pattern) {
		log.info("Parsing `{}` using `{}`", value, pattern);

		List<CDateRange> ranges = pattern.matcher(value)
				.results()
				.map(mr -> parseToCDateRange(mr.group(1)))
				.collect(Collectors.toList());

		log.info("Parsed `{}`", ranges);

		return CDateSet.create(ranges);

	}

	private static Pattern generateDateSetPattern(@NonNull String setBegin, @NonNull String rangeSep, @NonNull String setEnd) {
		assert(setBegin.length() < 2);
		assert(rangeSep.length() >= 1);
		assert(setEnd.length() < 2);

		/*
		 Create a matcher pattern, that captures the date ranges in group 1 (the only group that is captured and which
		 is not allowed to hold any of the set-delimiters)

		 Groups starting with "?:" are not captured. The format parameters are reused in the format string by positional
		 reference "%X$s" where X refers to the position of the argument in String.format(...).
		 */
		return Pattern.compile(String.format("^(?:(?:%1$s)|(?:%2$s\\s*))([^%1$s%2$s%3$s]+)(?:%3$s)?$",
				setBegin.isEmpty() ? "" : Pattern.quote(setBegin), // referenced as: %1$s
				Pattern.quote(rangeSep.trim()),  // referenced as: %2$s, ignore white spaces as they are explicitly captured in the regex
				setEnd.isEmpty() ? "" : Pattern.quote(setEnd)));  // referenced as: %3$s
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
