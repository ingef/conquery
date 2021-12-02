package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.Objects;

/**
 * Pair with unordered elements ==> (a,b) = (b,a)
 */
public class UnorderedPair<A, B> {
	final A first;
	final B second;

	public UnorderedPair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UnorderedPair)) {
			return false;
		}
		UnorderedPair<A, B> up = (UnorderedPair<A, B>) o;
		return (up.first == this.first && up.second == this.second) ||
			   (up.first == this.second && up.second == this.first);
	}

	@Override
	public int hashCode() {
		final int firstHashCode = first.hashCode();
		final int secondHashCode = second.hashCode();
		return Objects.hash(Math.min(firstHashCode, secondHashCode), Math.max(firstHashCode, secondHashCode));
	}
}
