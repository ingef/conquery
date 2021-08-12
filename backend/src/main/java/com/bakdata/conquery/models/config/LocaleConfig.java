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

	@NotNull
	private Map<Locale, String> dateFormatMapping = Map.of(
			Locale.GERMAN, "dd.MM.yyyy",
			Locale.ROOT, "yyyy-MM-dd"
	);


	@NotNull
	private Set<String> parsingDateFormats = Set.of(
			"yyyy-MM-dd",
			"yyyyMMdd",
			"dd.MM.yyyy"
	);

	@NotEmpty
	private Map<Locale, String> localeRangeStartEndSeperators = Map.of(
			Locale.GERMAN, "-",
			Locale.ROOT, "/"
	);

	private Set<String> parsingRangeStartEndSeperators = Set.of("/");

	@NotNull
	@NotEmpty
	private List<ListFormat> listFormats = List.of(
			new ListFormat("", ", ", ""),
			new ListFormat("{", ", ", "}"));

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
		final ArrayList<String> rangeStartEndSeperators = new ArrayList<>(localeRangeStartEndSeperators.values());
		rangeStartEndSeperators.addAll(parsingRangeStartEndSeperators);
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
		final String closestMatch = findClosestMatch(locale, localeRangeStartEndSeperators);
		return closestMatch != null ? closestMatch : "/";
	}

	/**
	 * Finds the best formatter according to the locale and mapped date formatters.
	 * If there is no perfect match, the locale is abstracted, see findClosestMatch.
	 */
	public DateTimeFormatter findDateTimeFormater(Locale locale) {
		final String closestMatch = findClosestMatch(locale, dateFormatMapping);
		return closestMatch != null ? DateTimeFormatter.ofPattern(closestMatch) : DateTimeFormatter.ISO_DATE;
	}

	/**
	 * Adapted from {@link c10n.share.DefaultLocaleMapping}
	 */
	private static <T> T findClosestMatch(Locale forLocale, Map<Locale,T> options) {
		Set<Locale> fromSet = options.keySet();
		String variant = forLocale.getDisplayVariant();
		String country = forLocale.getCountry();
		String language = forLocale.getLanguage();
		List<Locale> c = new ArrayList<>(4);
		if (null != variant && !variant.isEmpty()) {
			c.add(forLocale);
		}
		if (null != country && !country.isEmpty()) {
			c.add(new Locale(language, country));
		}
		if (null != language && !language.isEmpty()) {
			c.add(new Locale(language));
		}
		for (Locale candidateLocale : c) {
			if (fromSet.contains(candidateLocale)) {
				return options.get(candidateLocale);
			}
		}
		return null;
	}


}
