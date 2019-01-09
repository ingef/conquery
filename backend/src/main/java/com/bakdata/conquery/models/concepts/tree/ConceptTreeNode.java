package com.bakdata.conquery.models.concepts.tree;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

public interface ConceptTreeNode<ID extends ConceptElementId<? extends ConceptElement<? extends ID>>> extends Named<ID> {

	@JsonManagedReference
	List<ConceptTreeChild> getChildren();
	public int getLocalId();
	public int getDepth();
	public int[] getPrefix();
	@JsonBackReference
	public ConceptTreeNode getParent();
	public void setLocalId(int size);
	public void setDepth(int i);

	public TreeChildPrefixIndex getChildIndex();
	public void setChildIndex(TreeChildPrefixIndex childIndex);
	
	public MatchingStats getMatchingStats();
	
	public default ConceptTreeNode<?> getElementByLocalId(int[] ids) {
		return getElementByLocalId(ids, 0);
	}
	
	public default ConceptTreeNode<?> getElementByLocalId(int[] ids, int index) {
		if(index == ids.length ) {
			return this;
		}
		else {
			return getChildren().get(ids[index]).getElementByLocalId(ids, index+1);
		}
	}
}
