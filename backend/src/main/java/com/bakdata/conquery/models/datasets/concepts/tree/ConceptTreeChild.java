package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
import lombok.Setter;

public class ConceptTreeChild extends ConceptElement<ConceptTreeChildId> implements ConceptTreeNode<ConceptTreeChildId> {

	@JsonIgnore
	private transient int[] prefix;
	@JsonManagedReference //@Valid
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
	private CTCondition condition = null;

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

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getConcept().getDataset();
	}


	public Optional<ConceptTreeNode<?>> findChildByDanglingIdComponents(List<Object> components) {

		if (components.isEmpty()) {
			return Optional.empty();
		}

		if (components.size() == 1) {
			// Check trivial case: one element
			if (components.get(0).equals(this.getId().getName())) {
				// "this" is the actual node
				return Optional.of(this);
			}
			else {
				return Optional.empty();
			}
		}

		// Check the children
		final Optional<ConceptTreeChild>
				optionalConceptTreeChild =
				children.stream().filter(child -> child.getId().getName().equals(components.get(1))).collect(MoreCollectors.toOptional());
		if (optionalConceptTreeChild.isEmpty()) {
			return Optional.empty();
		}

		// Components size is at least 2 here
		return optionalConceptTreeChild.flatMap(elem -> elem.findChildByDanglingIdComponents(components.subList(1, components.size())));
	}
}
