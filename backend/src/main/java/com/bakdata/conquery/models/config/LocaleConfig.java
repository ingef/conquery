package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import io.dropwizard.util.Strings;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
public class LocaleConfig {
	@NotNull
	private Locale frontend = Locale.ROOT;

	/**
	 * Mappings from user provided locale to date format which is used in the generation of result tables.
	 * The formats are also available for parsing dates using the {@link DateReader}. However, the locale is
	 * neglected there and the formats are tried until one that fits is found.
	 */
	@NotNull
	private Map<Locale, String> dateFormatMapping = Map.of(
			Locale.GERMAN, "dd.MM.yyyy",
			Locale.ROOT, "yyyy-MM-dd"
	);


	/**
	 * Additional date formats that are available only for parsing.
	 */
	@NotNull
	private Set<String> parsingDateFormats = Set.of(
			"yyyyMMdd"
	);

	/**
	 * Mappings from user provided locale to date range format which is used in the generation of result tables.
	 * The formats are also available for parsing dates ranges using the {@link DateReader}. However, the locale is
	 * neglected there and the formats are tried until one that fits is found.
	 */
	@NotEmpty
	private Map<Locale, String> localeRangeStartEndSeparators = Map.of(
			Locale.GERMAN, " - ",
			Locale.ROOT, "/"
	);

	/**
	 * Additional date range formats that are available only for parsing.
	 */
	private Set<String> parsingRangeStartEndSeparators = Set.of("/");

	/**
	 * List formats that are available for parsing inputs and (the first one) for rendering result tables.
	 * Spaces at the ends of the separator are only relevant for the output of results. For the input (parsing)
	 * the separator string can be surrounded by an arbitrary number of spaces.
	 */
	@NotNull
	@NotEmpty
	private List<ListFormat> listFormats = List.of(
			new ListFormat("", ", ", ""),
			new ListFormat("{", ",", "}"),
			new ListFormat("[", ",", "]")
	);

	/**
	 * Container to describe the format of a list
	 */
	@Data
	@RequiredArgsConstructor
	@Slf4j
	public static class ListFormat {
		@NonNull
		@Size(min = 0, max = 1)
		private final String start;
		@NonNull
		@Size(min = 1)
		private final String separator;

		@NonNull
		@Size(min = 0, max = 1)
		private final String end;

		private Pattern pattern;

		public Pattern getRegexPattern() {
			if (pattern != null) {
				return pattern;
			}
			/*
			 Create a matcher pattern, that captures the date ranges in group 1 (the only group that is captured and which
			 is not allowed to hold any of the set-delimiters)

			 Groups starting with "?:" are not captured. The format parameters are reused in the format string by positional
			 reference "%X$s" where X refers to the position of the argument in String.format(...).
			 */
			return pattern = Pattern.compile(String.format(
					"^(?:(?:%1$s)|(?:%2$s\\s*))([^%1$s%2$s%3$s]+)(?:%3$s)?$",
					getStart().isEmpty() ? "" : Pattern.quote(getStart()), // referenced as: %1$s
					Pattern.quote(getSeparator().trim()),  // referenced as: %2$s, ignore white spaces as they are explicitly captured in the regex
					getEnd().isEmpty() ? "" : Pattern.quote(getEnd())
			));  // referenced as: %3$s
		}

		public CDateSet parse(String value, DateReader reader) {
			final CDateSet out = CDateSet.create();

			StringBuffer buffer = new StringBuffer(value);

			require(getStart(), buffer);

			buffer.delete(0, getStart().length());

			while(buffer.length() > getEnd().length()){
				// trim leading whitespaces
				while(buffer.charAt(0) == ' '){
					buffer.delete(0, 1);
				}

				int end = buffer.indexOf(getSeparator());
				int next = end + getSeparator().length();

				if(end == -1){
					// No next separator found:
					// might be the last entry, might also be faulty entry
					if (!Strings.isNullOrEmpty(getEnd())) {
						end = buffer.indexOf(getEnd());
					}
					else {
						end = buffer.length();
					}
					next = end;
				}

				final String nextRange = buffer.substring(0, end);
				final CDateRange range = reader.parseToCDateRange(nextRange);

				out.add(range);

				buffer.delete(0, next);
			}

			require(getEnd(), buffer);

			buffer.delete(0, getEnd().length());

			if(buffer.length() != 0){
				throw new ParsingException("Trailing Data"); //TODO
			}

			return out;
		}

		private void require(String start, StringBuffer buffer) {
			if (!buffer.substring(0, start.length()).equals(start)) {
				throw new ParsingException("Expected Start but Got Actual"); //TODO
			}
		}
	}


	/**
	 * Date formats that are available for parsing.
	 */
	@JsonIgnore
	public DateReader getDateReader() {
		final List<String> rangeStartEndSeperators = new ArrayList<>(localeRangeStartEndSeparators.values());
		rangeStartEndSeperators.addAll(parsingRangeStartEndSeparators);
		return new DateReader(
				Sets.union(parsingDateFormats, Set.copyOf(dateFormatMapping.values())),
				rangeStartEndSeperators,
				listFormats
		);
	}

	/**
	 * Finds the best formatter according to the locale and mapped date formatters.
	 * If there is no perfect match, the locale is abstracted, see findClosestMatch.
	 */
	public String findDateRangeSeparator(Locale locale) {
		return findClosestMatch(locale, localeRangeStartEndSeparators, "/");
	}


	/**
	 * Finds the best date format according to the locale and mapped date formatters.
	 * If there is no perfect match, the locale is abstracted, see findClosestMatch.
	 */
	public String findDateFormat(Locale locale) {
		return findClosestMatch(locale, dateFormatMapping, "yyyy-MM-dd");
	}

	/**
	 * Helper method to find the best match for a given locale using its abstractions.
	 * First the vanilla locale is checked, then abstractions to country and language.
	 * The last resort is the {@link Locale#ROOT}. If no match is found, the alternative is returned.
	 */
	private static <T> T findClosestMatch(Locale forLocale, Map<Locale, T> options, T alternative) {
		String country = forLocale.getCountry();
		String language = forLocale.getLanguage();

		if (options.containsKey(forLocale)) {
			return options.get(forLocale);
		}

		Locale abstraction = new Locale(language, country);
		if (!country.isEmpty() && options.containsKey(abstraction)) {
			return options.get(abstraction);
		}

		abstraction = new Locale(language);
		if (!language.isEmpty() && options.containsKey(abstraction)) {
			return options.get(abstraction);
		}

		abstraction = Locale.ROOT;
		if (options.containsKey(abstraction)) {
			return options.get(abstraction);
		}

		return alternative;
	}


}
