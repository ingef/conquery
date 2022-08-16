package com.bakdata.conquery.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionsUtil {
	/**
	 * Create a probably smaller and similarly performing set of the incoming set.
	 *
	 * Use this for when you know those sets will no longer be touched and want to avoid memory overhead.
	 */
	public static <K> Set<K> createSmallestSet(Set<K> values) {
		final int size = values.size();

		if (size == 0) {
			return Collections.emptySet();
		}

		if (size == 1) {
			return Collections.singleton(values.iterator().next());
		}

		// TODO: 28.08.2020 fine tune this?
		if (size <= 100) {
			return new ObjectArraySet<>(values);
		}

		return values;
	}
}
