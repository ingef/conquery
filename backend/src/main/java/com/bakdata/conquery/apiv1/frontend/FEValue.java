package com.bakdata.conquery.apiv1.frontend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a values of a SELECT filter.
 */
@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class FEValue implements Comparable<FEValue> {
	private static final Comparator<FEValue> COMPARATOR = Comparator.comparing(FEValue::getValue)
																	.thenComparing(FEValue::getLabel);

	@NotNull
	private final String label;

	@NotNull
	private final String value;

	private String optionValue;

	public FEValue(@NotNull String label, @NotNull String value) {
		this.label = label;
		this.value = value;
	}

	/**
	 * Adds an item to the FilterSearch associating it with containing words.
	 * <p>
	 * The item is not added, if we've already collected an item with the same {@link FEValue#getValue()}.
	 */
	public List<String> extractKeywords() {
		final List<String> keywords = new ArrayList<>(3);

		keywords.add(getLabel());
		keywords.add(getValue());

		if (getOptionValue() != null) {
			keywords.add(getOptionValue());
		}
		return keywords;
	}

	@Override
	public int compareTo(@NotNull FEValue o) {
		return COMPARATOR.compare(this, o);
	}
}
