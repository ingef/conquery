package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a single node or concept in a concept tree.
 */
@Slf4j
@CPSType(id = "TREE", base = Concept.class)
@JsonDeserialize(converter = TreeConcept.TreeConceptInitializer.class)
public class TreeConcept extends Concept<ConceptTreeConnector> implements ConceptTreeNode<ConceptId>, SelectHolder<UniversalSelect>, Initializing<TreeConcept> {

	@JsonIgnore
	@Getter
	private final int depth = 0;

	@Getter
	private final int[] prefix = new int[]{0};

	@JsonIgnore
	private final List<ConceptTreeNode<?>> localIdMap = new ArrayList<>();

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
	private final Map<ImportId, ConceptTreeCache> caches = new ConcurrentHashMap<>();

	@Override
	public Concept<?> findConcept() {
		return getConcept();
	}

	public ConceptTreeCache getCache(ImportId imp) {
		return caches.get(imp);
	}

	@Override
	public ConceptTreeNode<?> getParent() {
		return null;
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

	public TreeConcept init() {

		final List<ConceptTreeChild> openList = new ArrayList<>(getChildren());

		for (ConceptTreeConnector con : getConnectors()) {
			if (con.getCondition() == null) {
				continue;
			}

			try {
				con.getCondition().init(this);
			} catch (ConceptConfigurationException e) {
				throw new RuntimeException("Unable to init condition", e);
			}
		}

		localIdMap.clear();
		setLocalId(0);
		localIdMap.add(this);

		for (int i = 0; i < openList.size(); i++) {
			final ConceptTreeChild ctc = openList.get(i);

			try {
				ctc.setLocalId(localIdMap.size());
				localIdMap.add(ctc);
				ctc.setDepth(ctc.getParent().getDepth() + 1);

				ctc.init();

			} catch (Exception e) {
				throw new RuntimeException("Error trying to consolidate the node " + ctc.getLabel() + " in " + getLabel(), e);
			}

			openList.addAll((openList.get(i)).getChildren());
		}

		return this;
	}

	public ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		return findMostSpecificChild(stringValue, rowMap, getChildren());
	}

	private ConceptTreeChild findMostSpecificChild(
			String stringValue,
			CalculatedValue<Map<String, Object>> rowMap,

			List<ConceptTreeChild> currentList
	)
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
							, stringValue, match.getId(), n.getId(), n.getConcept().getId(), rowMap.getValue());
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

	@JsonIgnore
	public Stream<ConceptTreeChild> getAllChildren() {
		return localIdMap.stream().filter(ConceptTreeChild.class::isInstance).map(ConceptTreeChild.class::cast);
	}

	@JsonIgnore
	private int nChildren = -1;

	@Override
	@JsonIgnore
	public int countElements() {
		if (nChildren > 0) {
			return nChildren;
		}

		return nChildren = 1 + (int) getAllChildren().count();
	}

	public void initializeIdCache(ImportId importId) {
		caches.computeIfAbsent(importId, id -> new ConceptTreeCache(this));
	}

	public void removeImportCache(ImportId imp) {
		caches.remove(imp);
	}

	/**
	 * Method to get the element of this concept tree that has the specified local ID.
	 * This should only be used by the query engine itself as an index.
	 *
	 * @param ids the local id array to look for
	 * @return the element matching the most specific local id in the array
	 */
	public ConceptTreeNode<?> getElementByLocalIdPath(int[] ids) {
		final int mostSpecific = ids[ids.length - 1];
		return getElementByLocalId(mostSpecific);
	}

	public ConceptTreeNode<?> getElementByLocalId(int localId) {
		return localIdMap.get(localId);
	}

	/**
	 * rawValue is expected to be an Integer, expressing a localId for {@link TreeConcept#getElementByLocalId(int)}.
	 * <p>
	 * If {@link PrintSettings#isPrettyPrint()} is true, {@link ConceptElement#getLabel()} is used to print.
	 * If {@link PrintSettings#isPrettyPrint()} is false, {@link ConceptElement#getId()} is used to print.
	 */
	public String printConceptLocalId(Object rawValue, PrintSettings printSettings) {

		if (rawValue == null) {
			return null;
		}

		final int localId = (int) rawValue;

		final ConceptTreeNode<?> node = getElementByLocalId(localId);

		if (!printSettings.isPrettyPrint()) {
			return node.getId().toString();
		}

		if (node.getDescription() == null) {
			return node.getLabel();
		}

		return node.getLabel() + " - " + node.getDescription();

	}


	public ConceptElement<? extends ConceptElementId<? extends ConceptElement<?>>> findById(ConceptElementId<?> id) {
		List<Object> parts = new ArrayList<>();
		id.collectComponents(parts);
		final ConceptId conceptId = getId();
		if (!parts.subList(0, 2).equals(conceptId.getComponents())) {
			return null;
		}
		if (parts.size() == 2) {
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

	public static class TreeConceptInitializer extends Initializing.Converter<TreeConcept> {}
}
