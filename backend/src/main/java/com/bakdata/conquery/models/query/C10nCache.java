package com.bakdata.conquery.models.query;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import c10n.C10N;
import lombok.experimental.UtilityClass;

@UtilityClass
public class C10nCache {
	private static ConcurrentMap<Locale, ConcurrentMap<Class, Object>> cache = new ConcurrentHashMap<>();

	public <T> T getLocalized(Class<? super T> clazz, Locale locale) {
		return (T) cache.computeIfAbsent(locale, (ignored) -> new ConcurrentHashMap<>())
						.computeIfAbsent(clazz, ignored -> C10N.get(clazz, locale));
	}
}
