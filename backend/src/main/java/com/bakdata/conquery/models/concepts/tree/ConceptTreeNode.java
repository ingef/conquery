package com.bakdata.conquery.models.concepts.tree;

import java.util.List;

import com.bakdata.conquery.models.concepts.ConceptElement;
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
	//public void incMatchingEntries(CDateRange conceptDateRange);
	public void setLocalId(int size);
	public void setDepth(int i);

	public TreeChildPrefixIndex getChildIndex();
	public void setChildIndex(TreeChildPrefixIndex childIndex);
}
