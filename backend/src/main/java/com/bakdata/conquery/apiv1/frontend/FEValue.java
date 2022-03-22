package com.bakdata.conquery.apiv1.frontend;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a values of a SELECT filter.
 */
@Data
public class FEValue implements Comparable<FEValue> {
	private static final Comparator<FEValue> COMPARATOR = Comparator.comparing(FEValue::getValue)
																	.thenComparing(FEValue::getLabel);

	private final String label;

	@NotNull
	private final String value;

	private final String optionValue;

	@JsonCreator
	public FEValue(String label, String value, String optionValue) {
		this.value = value;
		this.label = Objects.requireNonNullElse(label, value);
		this.optionValue = optionValue;
	}

	public FEValue(String label, String value) {
		this(label, value, null);
	}

	/**
	 * Adds an item to the FilterSearch associating it with containing words.
	 * <p>
	 * The item is not added, if we've already collected an item with the same {@link FEValue#getValue()}.
	 */
	public List<String> extractKeywords() {
		final ImmutableList.Builder<String> builder = ImmutableList.builderWithExpectedSize(3);

		builder.add(getLabel())
			   .add(getValue());

		if (getOptionValue() != null) {
			builder.add(getOptionValue());
		}

		return builder.build();
	}

	@Override
	public int compareTo(@NotNull FEValue o) {
		return COMPARATOR.compare(this, o);
	}
}
