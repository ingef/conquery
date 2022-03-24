package com.bakdata.conquery.apiv1.frontend;

import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a values of a SELECT filter.
 */
@Data
public class FEValue implements Comparable<FEValue> {
	private static final Comparator<FEValue> COMPARATOR = Comparator.comparing(FEValue::getValue)
																	.thenComparing(FEValue::getLabel);

	/**
	 * Value is the only relevant data-point for hashing/equality and searching from the service perspective.
	 */
	@NotNull
	@EqualsAndHashCode.Include
	private final String value;

	@EqualsAndHashCode.Exclude
	@NotNull
	private final String label;

	@EqualsAndHashCode.Exclude
	private final String optionValue;

	@JsonCreator
	public FEValue(@NonNull String value, @NonNull String label, String optionValue) {
		this.value = value;
		this.label = Objects.requireNonNullElse(label, value);
		this.optionValue = optionValue;
	}

	public FEValue(String value, String label) {
		this(value, label, null);
	}

	@Override
	public int compareTo(@NotNull FEValue o) {
		return COMPARATOR.compare(this, o);
	}
}
