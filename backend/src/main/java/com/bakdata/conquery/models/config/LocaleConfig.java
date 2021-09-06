package com.bakdata.conquery.models.config;

import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import lombok.*;

@Getter @Setter
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
			new ListFormat("[", ",", "]"));

	/**
	 * Container to describe the format of a list
	 */
	@Data
	@AllArgsConstructor
	public static class ListFormat {
		@NonNull @Max(1)
		String start;
		@NonNull @Min(1)
		String separator;
		@NonNull @Max(1)
		String end;
	}


	/**
	 * Date formats that are available for parsing.
	 */
	@JsonIgnore
	public DateReader getDateReader() {
		final ArrayList<String> rangeStartEndSeperators = new ArrayList<>(localeRangeStartEndSeparators.values());
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
	private static <T> T findClosestMatch(Locale forLocale, Map<Locale,T> options, T alternative) {
		String country = forLocale.getCountry();
		String language = forLocale.getLanguage();

		if (options.containsKey(forLocale)){
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
