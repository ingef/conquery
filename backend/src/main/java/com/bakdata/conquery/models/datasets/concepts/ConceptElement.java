package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class ConceptElement<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>> extends Labeled<ID> implements NamespacedIdentifiable<ID> {

	private String description;
	private List<KeyValue> additionalInfos = Collections.emptyList();

	/**
	 * Initialize this only when needed. It is not needed
	 */
	@CheckForNull
	private MatchingStats matchingStats;

	public abstract void clearMatchingStats();

	@Override
	public String toString() {
		return String.format("%s[%s]", this.getClass().getSimpleName(), getLabel());
	}

	@JsonIgnore
	public abstract Concept<?> getConcept();

	public abstract int[] getPrefix();

	public abstract boolean matchesPrefix(int[] conceptPrefix);

	@Override
	public abstract ID createId();


}
