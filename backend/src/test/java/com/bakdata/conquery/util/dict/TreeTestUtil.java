package com.bakdata.conquery.util.dict;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TreeTestUtil {

	public static <T> Comparator<T> shuffle(Random random) {
		final Map<Object, UUID> uniqueIds = new IdentityHashMap<>();

		return Comparator.comparing(e -> uniqueIds.computeIfAbsent(e, ignored -> {
			final byte[] randomBytes = new byte[16];
			random.nextBytes(randomBytes);

			return UUID.nameUUIDFromBytes(randomBytes);
		}));
	}

}
