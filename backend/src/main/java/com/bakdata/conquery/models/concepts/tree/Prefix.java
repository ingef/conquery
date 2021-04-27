package com.bakdata.conquery.models.concepts.tree;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class implementing String-compareTo only for prefixes.
 */
@Data
public class Prefix implements Comparable<Prefix> {

	@JsonValue
	@NotNull
	private final String value;

	@Override
	public int compareTo(Prefix other) {
		final int min = Math.min(getValue().length(), other.getValue().length());

		return getValue().substring(0, min).compareTo(other.getValue().substring(0, min));
	}
}
