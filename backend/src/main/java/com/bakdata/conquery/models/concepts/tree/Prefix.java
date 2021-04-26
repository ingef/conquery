package com.bakdata.conquery.models.concepts.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Prefix implements Comparable<Prefix> {
	@JsonCreator
	public static Prefix of(String value){
		return new Prefix(value);
	}

	@JsonValue
	@NotNull
	private final String value;

	@Override
	public int compareTo(@NotNull Prefix o) {
		final int min = Math.min(getValue().length(), o.getValue().length());

		return getValue().substring(0, min).compareToIgnoreCase(o.getValue().substring(0,min));
	}
}
