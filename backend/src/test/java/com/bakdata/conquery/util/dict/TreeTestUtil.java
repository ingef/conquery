package com.bakdata.conquery.util.dict;

import java.util.Comparator;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TreeTestUtil {

	public static <T> Comparator<T> shuffle(Random random) {
		final Object2IntMap<T> uniqueIds = new Object2IntOpenHashMap<>();

		return Comparator.comparing(e -> uniqueIds.computeIfAbsent(e, ignored -> random.nextInt()));
	}

}
