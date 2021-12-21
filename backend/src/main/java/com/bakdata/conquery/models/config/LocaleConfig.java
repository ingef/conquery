package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

		/**
		 * Manually parse value as {@link CDateSet} using the reader for {@link CDateRange}.
		 */
		public CDateSet parse(String value, DateReader reader) {
			final CDateSet out = CDateSet.create();

			final StringBuffer buffer = new StringBuffer(value);

			boolean done = false;

			// Require that the string starts properly
			require(getStart(), buffer);

			buffer.delete(0, getStart().length());

			while(!done){
				// end is the end of what we believe is the current CDateRange
				int end = buffer.indexOf(getSeparator());

				// next is where we will advance to after parsing the CDateRange:
				// Either past the next separator, or to the end if there is no next separator
				int next = end + getSeparator().length();

				if(end == -1){
					// No next separator found:
					// might be the last entry, might also be faulty entry
					// Either way, parsing should stop after this entry.

					end = findEnd(buffer);

					// There is no end, we can abort now
					if(end == -1){
						throw new ParsingException("No end in sight");
					}

					next = end;
					done = true;
				}

				// Parse the substring (substring is cheap as it will use offsets)
				final String nextRange = buffer.substring(0, end).trim();
				final CDateRange range = reader.parseToCDateRange(nextRange);

				out.add(range);

				// advance beyond the parsed range
				buffer.delete(0, next);
			}

			// require that the Set ends properly, and has nothing beyond the end character
			require(getEnd(), buffer);

			buffer.delete(0, getEnd().length());

			assertEmpty(buffer);

			return out;
		}

		private int findEnd(StringBuffer buffer) {
			if (Strings.isNullOrEmpty(getEnd())) {
				return buffer.length();
			}

			return buffer.indexOf(getEnd());
		}

		private void assertEmpty(StringBuffer buffer) {
			if(buffer.length() != 0){
				throw new ParsingException(String.format("Trailing Data `%s` when using Format %s", buffer, this));
			}
		}

		private void require(String expected, StringBuffer buffer) {
			final String actual = buffer.substring(0, expected.length());

			if (!actual.equals(expected)) {
				throw new ParsingException(String.format("Expected `%s` but got `%s`", expected, actual));
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
