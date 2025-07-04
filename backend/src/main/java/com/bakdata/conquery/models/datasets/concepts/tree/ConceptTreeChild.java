package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

public class ConceptTreeChild extends ConceptElement<ConceptTreeChildId> {

	@JsonIgnore
	private transient int[] prefix;

	@JsonManagedReference
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
	@EqualsAndHashCode.Exclude
	private ConceptElement<?> parent;
	@JsonIgnore
	@Getter
	@Setter
	private int depth = 0;
	@Getter
	@NotNull
	@Setter
	private CTCondition condition = null;


	@Override
	public void clearMatchingStats() {
		setMatchingStats(null);
	}

	public void init() throws ConceptConfigurationException {
		if (condition != null) {
			condition.init(this);
		}
	}

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return getConcept().getDataset();
	}

	@Override
	@JsonIgnore
	public TreeConcept getConcept() {
		ConceptElement<?> n = this;
		while (n != null) {
			if (n instanceof TreeConcept) {
				return (TreeConcept) n;
			}
			n = n.getParent();
		}
		throw new IllegalStateException("The node " + this + " seems to have no root");
	}

	@JsonIgnore
	public int[] getPrefix() {
		if (prefix == null) {
			int[] pPrefix = getParent().getPrefix();
			prefix = Arrays.copyOf(pPrefix, pPrefix.length + 1);
			prefix[prefix.length - 1] = this.getLocalId();
		}
		return prefix;
	}

	@Override
	public boolean matchesPrefix(int[] conceptPrefix) {
		return conceptPrefix.length > depth && conceptPrefix[depth] == localId;
	}

	@Override
	public ConceptTreeChildId createId() {
		return new ConceptTreeChildId(parent.getId(), getName());
	}

	/**
	 * Parts only contains references to child elements.
	 * If parts is empty return self.
	 * If the first part does not match the name of a child return null
	 */
	ConceptTreeChild findByParts(List<Object> parts) {
		if (parts.isEmpty()) {
			return this;
		}

		for (ConceptTreeChild child : children) {
			if (parts.get(0).equals(child.getName())) {
				final List<Object> subList = parts.size() > 1 ? parts.subList(1, parts.size()) : Collections.emptyList();
				return child.findByParts(subList);
			}
		}
		return null;
	}
}
