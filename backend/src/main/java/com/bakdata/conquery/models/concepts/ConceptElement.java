package com.bakdata.conquery.models.concepts;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.bakdata.conquery.models.common.KeyValue;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

public abstract class ConceptElement<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>> extends Labeled<ID> {

	@Getter @Setter
	private String description;
	@Getter @Setter
	private List<KeyValue> additionalInfos = Collections.emptyList();
	@Getter @Setter @JsonIgnore
	private MatchingStats matchingStats = new MatchingStats();

	public ConceptElement<?> getElementById(ConceptElementId<?> conceptElementId) {
		if(Objects.equals(conceptElementId, this.getId())) {
			return this;
		}

		if(conceptElementId instanceof ConceptTreeChildId) {
			return getChildById((ConceptTreeChildId)conceptElementId);
		}

		throw new NoSuchElementException("Could not resolve the element "+conceptElementId +" in " + this);
	}

	public ConceptTreeChild getChildById(ConceptTreeChildId conceptTreeChildId) {
		throw new UnsupportedOperationException("The concept "+this+" has no children. Was looking for "+conceptTreeChildId);
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", this.getClass().getSimpleName(), getLabel());
	}
	
	@JsonIgnore
	public abstract Concept<?> getConcept();

	public abstract boolean matchesPrefix(int[] conceptPrefix);

	public abstract long calculateBitMask();
}
