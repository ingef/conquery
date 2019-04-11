package com.bakdata.conquery.util;

import java.util.Collections;
import java.util.PrimitiveIterator;
import java.util.function.Predicate;

import com.bakdata.conquery.models.common.Range;

public class RangeUtil {

	public static <T extends Comparable<T>> Predicate<T> lessThan(T reference){
		return value -> value.compareTo(reference) < 0;
	}

	public static <T extends Comparable<T>> Predicate<T> atMost(T reference){
		return value -> value.compareTo(reference) <= 0;
	}

	public static <T extends Comparable<T>> Predicate<T> biggerThan(T reference){
		return value -> value.compareTo(reference) > 0;
	}

	public static <T extends Comparable<T>> Predicate<T> atLeast(T reference){
		return value -> value.compareTo(reference) >= 0;
	}

	public static <T extends Comparable<T>> Predicate<T> exactly(T reference){
		return value -> value.compareTo(reference) == 0;
	}

	public static Iterable<Integer> iterate(Range<Integer> range) {
		if(range == null) {
			return Collections.emptyList();
		}
		if (range.isOpen()) {
			throw new IllegalStateException("Range " + range + " is not bounded.");
		}

		final int min = range.getMin();
		final int max = range.getMax();

		return () -> new PrimitiveIterator.OfInt() {
			private int current = min;

			@Override
			public boolean hasNext() {
				return current <= max;
			}

			@Override
			public int nextInt() {
				return current++;
			}

		};
	}

}
