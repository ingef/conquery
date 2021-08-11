package com.bakdata.conquery.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing multiple dateformats. Parsing is cached in two ways: First parsed values are cached. Second, the last used parser is cached since it's likely that it will be used again, we therefore try to use it first, then try all others.
 */
@Slf4j
@NoArgsConstructor
public class DateReader {

	/**
	 * All available formats for parsing.
	 */
	@JsonIgnore
	private Set<DateTimeFormatter> dateFormats;

	/**
	 * Last successfully parsed dateformat.
	 */
	@JsonIgnore
	private final ThreadLocal<DateTimeFormatter> lastFormat = new ThreadLocal<>();

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
	public DateReader(@NotEmpty Set<String> dateParsingFormats) {
		this.dateFormats = dateParsingFormats.stream().map(DateTimeFormatter::ofPattern).collect(Collectors.toSet());
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
			throw new IllegalArgumentException(String.format("Failed to parse `%s` as LocalDate.", value));
		}

		return out;
	}

	/**
	 * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
	 * <p>
	 * Method is private as it is only directly accessed via the Cache.
	 */
	private LocalDate tryParse(String value) {

		final DateTimeFormatter formatter = lastFormat.get();

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
					lastFormat.set(format);
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
