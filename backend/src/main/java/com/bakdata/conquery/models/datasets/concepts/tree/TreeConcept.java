package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a single node or concept in a concept tree.
 */
@Slf4j
@CPSType(id = "TREE", base = Concept.class)
@JsonDeserialize(converter = TreeConcept.Initializer.class)
@EqualsAndHashCode(callSuper=true)
public class TreeConcept extends Concept<ConceptTreeConnector> implements SelectHolder<UniversalSelect> {

	@JsonIgnore
	@Getter
	@EqualsAndHashCode.Exclude
	private final int depth = 0;

	@Getter
	private final int[] prefix = new int[]{0};


	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private final List<ConceptElement<?>> localIdMap = new ArrayList<>();
	@Getter
	@Setter
	@Valid
	private List<ConceptTreeChild> children = Collections.emptyList();
	@View.Internal
	@Getter
	@Setter
	private int localId;
	@NotNull
	@Getter
	@Setter
	@Valid
	@JsonManagedReference
	private List<UniversalSelect> selects = new ArrayList<>();
	@JsonIgnore
	private int nChildren = -1;

	@Override
	public Concept<?> findConcept() {
		return getConcept();
	}


	@Override
	public void clearMatchingStats() {
		setMatchingStats(null);
		getAllChildren().forEach(ConceptTreeChild::clearMatchingStats);
	}

	@Override
	public boolean matchesPrefix(int[] conceptPrefix) {
		return conceptPrefix != null && conceptPrefix[0] == 0;
	}

	@JsonIgnore
	public Stream<ConceptTreeChild> getAllChildren() {
		return localIdMap.stream().filter(ConceptTreeChild.class::isInstance).map(ConceptTreeChild.class::cast);
	}

	public ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		return findMostSpecificChild(stringValue, rowMap, getChildren());
	}

	private ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap, List<ConceptTreeChild> currentList)
			throws ConceptConfigurationException {

		ConceptTreeChild best = null;

		while (currentList != null && !currentList.isEmpty()) {
			ConceptTreeChild match = null;
			boolean failed = false;
			for (ConceptTreeChild n : currentList) {
				if (!n.getCondition().matches(stringValue, rowMap)) {
					continue;
				}

				if (match != null) {
					failed = true;
					log.error("Value '{}' matches the two nodes {} and {} in the tree {} (row={}))"
							, stringValue, match.getId(), n.getId(), n.getConcept().getId(), rowMap.getValue()
					);
					continue;
				}

				match = n;

			}

			if (failed) {
				return null;
			}

			// Nothing better found below, so return best-so far match
			if (match == null) {
				return best;
			}

			best = match;
			currentList = match.getChildren();
		}
		return best;
	}


	/**
	 * Method to get the element of this concept tree that has the specified local ID.
	 * This should only be used by the query engine itself as an index.
	 *
	 * @param ids the local id array to look for
	 * @return the element matching the most specific local id in the array
	 */
	public ConceptElement<?> getElementByLocalIdPath(int[] ids) {
		final int mostSpecific = ids[ids.length - 1];
		return getElementByLocalId(mostSpecific);
	}

	public ConceptElement<?> getElementByLocalId(int localId) {
		return localIdMap.get(localId);
	}

	@Override
	public void init() throws Exception {
		super.init();
		initElements();
	}

	@Override
	public void initElements() throws ConfigurationException, JSONException {
		super.initElements();
		setLocalId(0);
		localIdMap.add(this);

		final List<ConceptTreeChild> openList = new ArrayList<>(getChildren());

		for (ConceptTreeConnector con : getConnectors()) {
			if (con.getCondition() == null) {
				continue;
			}

			con.getCondition().init(this);
		}

		for (int i = 0; i < openList.size(); i++) {
			final ConceptTreeChild ctc = openList.get(i);

			try {
				ctc.setLocalId(localIdMap.size());
				localIdMap.add(ctc);
				ctc.setDepth(ctc.getParent().getDepth() + 1);

				ctc.init();
			}
			catch (Exception e) {
				throw new RuntimeException("Error trying to consolidate the node %s in %s".formatted(ctc.getLabel(), getLabel()), e);
			}

			openList.addAll((openList.get(i)).getChildren());
		}
	}

	@Override
	@JsonIgnore
	public int countElements() {
		if (nChildren > 0) {
			return nChildren;
		}

		return nChildren = 1 + (int) getAllChildren().count();
	}

	@Override
	public ConceptElement<? extends ConceptElementId<?>> findById(ConceptElementId<?> id) {
		List<Object> parts = new ArrayList<>();
		id.collectComponents(parts);

		List<Object> components = getId().getComponents();

		// Check if dataset and concept name match
		if (!(parts.get(0).equals(components.get(0)) && parts.get(1).equals(components.get(1)))) {
			return null;
		}

		if (parts.size() == 2) {
			// Perfect match <3
			return this;
		}

		for (ConceptTreeChild child : children) {
			if (parts.get(2).equals(child.getName())) {
				final List<Object> subParts = parts.size() > 3 ? parts.subList(3, parts.size()) : Collections.emptyList();
				return child.findByParts(subParts);
			}
		}

		return null;
	}

	public static class Initializer extends Initializing.Converter<TreeConcept> {
	}
}
