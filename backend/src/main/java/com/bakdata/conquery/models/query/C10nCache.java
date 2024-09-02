package com.bakdata.conquery.models.query;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import c10n.C10N;
import lombok.experimental.UtilityClass;

@UtilityClass
public class C10nCache {
	private static Map<Locale, Map<Class, Object>> cache = new HashMap<>();

	public <T> T getLocalized(Class<? super T> clazz, Locale locale) {
		return (T) cache.computeIfAbsent(locale, (ignored) -> new HashMap<>())
						.computeIfAbsent(clazz, ignored -> C10N.get(clazz, locale));
	}
}
