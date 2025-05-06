package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.ids.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class ConceptElement<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>>
		extends LabeledNamespaceIdentifiable<ID> {

	private String description;
	private List<KeyValue> additionalInfos = Collections.emptyList();

	/**
	 * Initialize this only when needed. It is not needed
	 */
	@CheckForNull
	private MatchingStats matchingStats;

	public abstract void clearMatchingStats();

	@JsonIgnore
	public abstract Concept<?> getConcept();

	@JsonIgnore
	public abstract int[] getPrefix();

	public abstract boolean matchesPrefix(int[] conceptPrefix);


	@JsonManagedReference
	public abstract List<ConceptTreeChild> getChildren();

	public abstract int getLocalId();

	public abstract int getDepth();

	@JsonBackReference
	public abstract ConceptElement<?> getParent();

	public abstract void setLocalId(int size);

}
