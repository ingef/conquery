package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.MoreCollectors;

public interface ConceptTreeNode<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>> extends Named<ID> {

	@JsonManagedReference
	List<ConceptTreeChild> getChildren();
	int getLocalId();
	int getDepth();

	@JsonIgnore
	int[] getPrefix();

	@JsonBackReference
	ConceptTreeNode getParent();

	void setLocalId(int size);

	TreeChildPrefixIndex getChildIndex();

	void setChildIndex(TreeChildPrefixIndex childIndex);

	MatchingStats getMatchingStats();


	default Optional<ConceptTreeNode<?>> findChildById(ConceptElementId<?> id) {
		final List<Object> thisComponents = getId().getComponents();
		final List<Object> otherComponents = id.getComponents();

		if (thisComponents.size() > otherComponents.size()) {
			// The other id is too unspecific for this nested child
			return Optional.empty();
		}

		int walk = 0;

		// Check all components in the id length of "this" for equality
		while (walk < thisComponents.size()) {
			if (!thisComponents.get(walk).equals(otherComponents.get(walk))) {
				return Optional.empty();
			}
			walk++;
		}

		if (walk == otherComponents.size()) {
			// "this" is the actual node that was searched
			return Optional.of(this);
		}

		final int childComponentStart = walk;
		// Search in children
		final Optional<ConceptTreeChild>
				mayBeChild =
				getChildren().stream()
							 .filter(child -> child.getId().getName().equals(otherComponents.get(childComponentStart)))
							 .collect(MoreCollectors.toOptional());

		if (mayBeChild.isEmpty()) {
			// No child had a fitting name
			return Optional.empty();
		}
		return mayBeChild.get().findChildByDanglingIdComponents(otherComponents.subList(childComponentStart, otherComponents.size()));

	}
}
