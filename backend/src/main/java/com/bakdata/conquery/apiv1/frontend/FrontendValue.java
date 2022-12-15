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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FrontendValue implements Comparable<FrontendValue> {
	private static final Comparator<FrontendValue> COMPARATOR = Comparator.comparing(FrontendValue::getValue)
																		  .thenComparing(FrontendValue::getLabel);

	/**
	 * Value is the only relevant data-point for hashing/equality and searching from the service perspective.
	 */
	@NotNull
	@EqualsAndHashCode.Include
	private final String value;

	@NotNull
	private final String label;

	private final String optionValue;

	@JsonCreator
	public FrontendValue(@NonNull String value, @NonNull String label, String optionValue) {
		this.value = value;
		this.label = Objects.requireNonNullElse(label, value);
		this.optionValue = optionValue;
	}

	public FrontendValue(String value, String label) {
		this(value, label, null);
	}

	@Override
	public int compareTo(@NotNull FrontendValue o) {
		return COMPARATOR.compare(this, o);
	}
}
