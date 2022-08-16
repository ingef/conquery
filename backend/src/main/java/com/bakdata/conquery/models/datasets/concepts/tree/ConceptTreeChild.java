package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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

	public Optional<ConceptTreeNode<?>> findChildById(ConceptElementId<?> id) {
		if (!(id instanceof ConceptTreeChildId)) {
			return Optional.empty();
		}

		if (getId().equals(id)) {
			return Optional.of(this);
		}

		// try to find the own id in the id chain
		ConceptElementId<?> prevIdWalk = null;
		ConceptElementId<?> idWalk = id;
		while (idWalk instanceof ConceptTreeChildId && !getId().equals(idWalk)) {
			prevIdWalk = idWalk;
			idWalk = ((ConceptTreeChildId) idWalk).getParent();
		}

		// Null-Check just for caution, prevIdWalk should never be null at this point
		if (getId().equals(idWalk) && prevIdWalk != null) {
			// find matching child
			final Map<ConceptTreeChildId, ConceptTreeChild>
					idMap =
					children.stream().collect(Collectors.toMap(ConceptTreeChild::getId, Function.identity()));
			final ConceptTreeChild child = idMap.get(prevIdWalk);

			if (child != null) {
				return child.findChildById(id);
			}

			// No matching child was found
		}

		return Optional.empty();
	}
}
