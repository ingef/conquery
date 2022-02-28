package com.bakdata.conquery.apiv1.frontend;

import java.util.Comparator;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a values of a SELECT filter.
 */
@Data
@AllArgsConstructor
public class FEValue implements Comparable<FEValue> {
	private static final Comparator<FEValue> COMPARATOR = Comparator.comparing(FEValue::getValue)
																	.thenComparing(FEValue::getLabel);

	@NotNull
	private final String label;

	@NotNull
	private final String value;

	private String optionValue;

	public FEValue(String label, String value) {
		this.label = label;
		this.value = value;
	}

	@Override
	public int compareTo(@NotNull FEValue o) {
		return COMPARATOR.compare(this, o);
	}
}
