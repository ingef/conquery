package com.bakdata.conquery.models.concepts.tree.validation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class implementing String-compareTo only for prefixes.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Prefix implements Comparable<Prefix> {

	public static Prefix prefix(String value){
		return new Prefix(Mode.PREFIX, value);
	}

	public static Prefix equal(String value){
		return new Prefix(Mode.EQUAL, value);
	}

	private static enum Mode {
		PREFIX, EQUAL
	}
	private final Mode mode;

	//TODO also implement Equals, always degrade to prefix when we mix
	@JsonValue
	@NotNull
	private final String value;

	@Override
	public int compareTo(Prefix other) {
		if(mode == Mode.PREFIX || other.mode == Mode.PREFIX) {
			final int min = Math.min(getValue().length(), other.getValue().length());

			return getValue().substring(0, min).compareTo(other.getValue().substring(0, min));
		}

		return getValue().compareTo(other.getValue());
	}
}
