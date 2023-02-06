package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.primitives.Ints;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Caches values of an iterator by spilling them into a backing array.
 * Access exceeding already observed values results in a spill, and a view of the list to the offsets is returned.
 */
@RequiredArgsConstructor
@ToString
public class Cursor<T> {
	private final Iterator<T> provider;
	private final List<T> past = new ArrayList<>();

	private synchronized void advanceTo(final int expectedSize) {
		while (currentSize() < expectedSize && provider.hasNext()) {
			past.add(provider.next());
		}
	}

	private int currentSize() {
		return past.size();
	}

	public List<T> get(int from, int to) {
		// Being inclusive in the API is easier to read
		final int end = Ints.saturatedCast((long) to + 1L);

		if (end > currentSize()) {
			advanceTo(end);
		}

		//TODO FK: I don't want to synchronize here but I don't actually know how Lists handle access while growing :( (It's also unlikely to have real collisions)

		final int currentSize = currentSize();
		// We have exceeded the available data
		if (from > currentSize) {
			return Collections.emptyList();
		}

		return past.subList(from, Math.min(end, currentSize));
	}
}
