package com.bakdata.conquery.models.identifiable;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.base.CharMatcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@ToString
public abstract class Labeled<ID extends IId<? extends Labeled<? extends ID>>> extends NamedImpl<ID> {

	private static final CharMatcher DEFAULT_NAME_UNWANTED = CharMatcher.is(IId.JOIN_CHAR).or(CharMatcher.whitespace());

	/**
	 * shown in the frontend
	 *
	 * @jsonExample "someLabel"
	 */
	private String label;


	//if only label or name is given the rest is inferred
	public final void setLabel(String label) {
		this.label = label;
		if (getName() == null) {
			setName(makeDefaultName(label));
		}
	}

	private String makeDefaultName(String label) {
		return DEFAULT_NAME_UNWANTED.replaceFrom(label.toLowerCase(), "_");
	}

	@NotEmpty
	@ToString.Include
	public String getLabel() {
		if (label == null) {
			return getName();
		}

		return label;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[label=" + label + ", name=" + getName() + "]";
	}

}
