package com.bakdata.conquery.sql.query.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModelNormalization {

	private ModelNormalization() {
	}

	public static <T> List<T> immutableCopy(List<T> values) {
		return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
	}

	public static <T> Set<T> immutableCopy(Set<T> values) {
		return values == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(values));
	}

	public static <K, V> Map<K, V> immutableCopy(Map<K, V> values) {
		return values == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}
}
