package com.bakdata.conquery.models.concepts.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.conditions.ConceptTreeCondition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.RangeSet;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConceptTreeChild extends ConceptElement<ConceptTreeChildId> implements ConceptTreeNode<ConceptTreeChildId> {

	@JsonIgnore
	private transient int[] prefix;

	@JsonManagedReference @Valid
	@Getter
	@Setter
	private List<ConceptTreeChild> children = Collections.emptyList();
	@JsonIgnore
	@Getter
	@Setter
	private int localId;
	@JsonBackReference
	@Getter
	@Setter
	private ConceptTreeNode<?> parent;
	@JsonIgnore
	@Getter
	@Setter
	private int depth = 0;
	@Getter
	@NotNull
	@Setter
	private ConceptTreeCondition condition = null;

	@JsonIgnore
	@Getter
	@Setter
	private TreeChildPrefixIndex childIndex;

	@Override
	@JsonIgnore
	public int[] getPrefix() {
		if (prefix == null) {
			int[] pPrefix = getParent().getPrefix();
			prefix = Arrays.copyOf(pPrefix, pPrefix.length + 1);
			prefix[prefix.length - 1] = this.getLocalId();
		}
		return prefix;
	}

	public void init() throws ConceptConfigurationException {
		if (condition != null) {
			condition.init(this);
		}
	}

	@Override
	public ConceptTreeChildId createId() {
		return new ConceptTreeChildId(parent.getId(), getName());
	}

	@Override
	@JsonIgnore
	public TreeConcept getConcept() {
		ConceptTreeNode<?> n = this;
		while (n != null) {
			if (n instanceof TreeConcept) {
				return (TreeConcept) n;
			}
			n = n.getParent();
		}
		throw new IllegalStateException("The node " + this + " seems to have no root");
	}

	@Override
	public boolean matchesPrefix(int[] conceptPrefix) {
		return conceptPrefix.length > depth && conceptPrefix[depth] == localId;
	}

	@Override
	public long calculateBitMask() {
		if (getLocalId() < 64) {
			return 1L << getLocalId();
		}
		return getParent().calculateBitMask();
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getConcept().getDataset();
	}

	@JsonIgnore
	private Map<String, RangeSet<Prefix>> columnSpan = null;

	@JsonIgnore
	public Map<String, RangeSet<Prefix>> getColumnSpan(){

		// This will walk the whole sub-tree so caching is important for performance
		if(columnSpan != null){
			return columnSpan;
		}

		final Map<String, RangeSet<Prefix>> span = new HashMap<>(condition.getColumnSpan());

		for (ConceptTreeChild child : children) {
			ConceptTreeCondition.mergeRanges(span,child.getColumnSpan());
		}

		return columnSpan = span;
	}


	@ValidationMethod
	@JsonIgnore
	public boolean isChildrenAreNonOverlapping() {

		for (int index = 0; index < children.size(); index++) {

			ConceptTreeChild child = children.get(index);
			final Map<String, RangeSet<Prefix>> childSpan = child.getColumnSpan();

			// Siblings may not overlap with each other
			for (int otherIndex = index + 1; otherIndex < children.size(); otherIndex++) {
				final ConceptTreeChild other = children.get(otherIndex);

				if (intersects(other.getColumnSpan(), childSpan)) {
					log.error("{} intersects with its Sibling {}", child, other);
					return false;
				}
			}
		}

		return true;
	}

	@ValidationMethod
	@JsonIgnore
	public boolean isEnclosingChildren() {
		final Map<String, RangeSet<Prefix>> mySpan = condition.getColumnSpan();

		for (ConceptTreeChild child : children) {

			final Map<String, RangeSet<Prefix>> childSpan = child.getColumnSpan();

			// Children have to be enclosed by parent
			if (!encloses(mySpan, childSpan)) {
				log.error("{} does not enclose Child {}", this, child);
				return false;
			}
		}

		return true;
	}

	private boolean encloses(Map<String, RangeSet<Prefix>> mySpan, Map<String, RangeSet<Prefix>> childSpan) {
		for (Map.Entry<String, RangeSet<Prefix>> entry : childSpan.entrySet()) {
			// Not defined spans everything
			if(!mySpan.containsKey(entry.getKey())){
				continue;
			}

			if(mySpan.get(entry.getKey()).enclosesAll(entry.getValue())){
				continue;
			}

			return false;
		}

		return true;
	}

	private static boolean intersects(Map<String, RangeSet<Prefix>> left, Map<String, RangeSet<Prefix>> right) {
		for (String column : left.keySet()) {
			if(!right.containsKey(column)){
				continue;
			}

			if(right.get(column).asRanges().stream().anyMatch(left.get(column)::intersects)){
				return true;
			}
		}

		return false;
	}
}
