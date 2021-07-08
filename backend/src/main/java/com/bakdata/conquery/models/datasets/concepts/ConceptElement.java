package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
public abstract class ConceptElement<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>> extends Labeled<ID> implements NamespacedIdentifiable<ID> {

	@Getter @Setter
	private String description;
	@Getter @Setter
	private List<KeyValue> additionalInfos = Collections.emptyList();
	@Getter @Setter @JsonIgnore
	private MatchingStats matchingStats = new MatchingStats();

	public ConceptTreeChild getChildById(ConceptTreeChildId conceptTreeChildId) {
		throw new UnsupportedOperationException("The concept "+this+" has no children. Was looking for "+conceptTreeChildId);
	}
	
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
