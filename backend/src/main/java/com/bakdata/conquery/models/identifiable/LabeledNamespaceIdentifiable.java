package com.bakdata.conquery.models.identifiable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.base.CharMatcher;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public abstract class LabeledNamespaceIdentifiable<ID extends NamespacedId<?>> extends NamespacedIdentifiable<ID> {
	private static final CharMatcher DEFAULT_NAME_UNWANTED = CharMatcher.is(IdUtil.JOIN_CHAR).or(CharMatcher.whitespace());


	/**
	 * shown in the frontend
	 *
	 * @jsonExample "someLabel"
	 */
	private String label;

	@Getter(onMethod_ = {@ToString.Include, @NotBlank})
	@Setter
	private String name;

	protected static String makeDefaultName(String label) {
		return DEFAULT_NAME_UNWANTED.replaceFrom(label.toLowerCase(), "_");
	}


	public final void setLabel(String label) {
		if (label == null) {
			return;
		}

		this.label = label;
		if (getName() == null) {
			setName(makeDefaultName(label));
		}
	}

	@NotEmpty
	@ToString.Include
	public String getLabel() {
		if (label == null) {
			return getName();
		}

		return label;
	}
}
