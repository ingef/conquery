package com.bakdata.conquery.models.concepts.tree;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.List;

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
	void setDepth(int i);
	long calculateBitMask();

	TreeChildPrefixIndex getChildIndex();
	void setChildIndex(TreeChildPrefixIndex childIndex);
	
	MatchingStats getMatchingStats();
}
