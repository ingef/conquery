package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a single node or concept in a concept tree.
 */
@Slf4j
@CPSType(id = "TREE", base = Concept.class)
public class TreeConcept extends Concept<ConceptTreeConnector> implements ConceptTreeNode<ConceptId>, SelectHolder<UniversalSelect> {

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

	@JsonIgnore
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
	@Getter
	@Setter
	private TreeChildPrefixIndex childIndex;
	@JsonIgnore
	private final Map<Import, ConceptTreeCache> caches = new ConcurrentHashMap<>();

	@Override
	public Concept<?> findConcept() {
		return getConcept();
	}

	public ConceptTreeCache getCache(Import imp) {
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

	@Override
	public void initElements() throws ConfigurationException, JSONException {
		super.initElements();
		this.setLocalId(0);
		localIdMap.add(this);

		List<ConceptTreeChild> openList = new ArrayList<>();
		openList.addAll(this.getChildren());

		for (ConceptTreeConnector con : getConnectors()) {
			if (con.getCondition() == null) {
				continue;
			}

			con.getCondition().init(this);
		}

		for (int i = 0; i < openList.size(); i++) {
			ConceptTreeChild ctc = openList.get(i);

			try {
				ctc.setLocalId(localIdMap.size());
				localIdMap.add(ctc);
				ctc.setDepth(ctc.getParent().getDepth() + 1);

				ctc.init();

			}
			catch (Exception e) {
				throw new RuntimeException("Error trying to consolidate the node " + ctc.getLabel() + " in " + this.getLabel(), e);
			}

			openList.addAll((openList.get(i)).getChildren());
		}
	}

	public ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		if (this.getChildIndex() != null) {
			ConceptTreeChild best = this.getChildIndex().findMostSpecificChild(stringValue);

			if (best != null) {
				return findMostSpecificChild(stringValue, rowMap, best, best.getChildren());
			}
		}

		return findMostSpecificChild(stringValue, rowMap, null, this.getChildren());
	}

	private ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap, ConceptTreeChild best, List<ConceptTreeChild> currentList)
			throws ConceptConfigurationException {
		while (currentList != null && !currentList.isEmpty()) {
			ConceptTreeChild match = null;
			boolean failed = false;
			for (ConceptTreeChild n : currentList) {
				if (!n.getCondition().matches(stringValue, rowMap)) {
					continue;
				}

				if (match == null) {
					match = n;

					if (n.getChildIndex() != null) {
						ConceptTreeChild specificChild = n.getChildIndex().findMostSpecificChild(stringValue);

						if (specificChild != null) {
							match = specificChild;
						}
					}
				}
				else {
					failed = true;
					log.error("Value '{}' matches the two nodes {} and {} in the tree {} (row={}))"
							, stringValue, match.getLabel(), n.getLabel(), n.getConcept().getLabel(), rowMap.getValue());
					// TODO Why don't we return null here and drop the `failed`-flag?
				}
			}

			if (failed) {
				return null;
			}
			else if (match != null) {
				best = match;
				currentList = match.getChildren();
			}
			else {
				break;
			}
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

	public void initializeIdCache(StringStore type, Import importId) {
		caches.computeIfAbsent(importId, id -> new ConceptTreeCache(this, type.size()));
	}

	public void removeImportCache(Import imp) {
		caches.remove(imp);
	}

	/**
	 * Method to get the element of this concept tree that has the specified local ID.
	 * This should only be used by the query engine itself as an index.
	 *
	 * @param ids the local id array to look for
	 * @return the element matching the most specific local id in the array
	 */
	public ConceptTreeNode<?> getElementByLocalIdPath(@NonNull int[] ids) {
		int mostSpecific = ids[ids.length - 1];
		return getElementByLocalId(mostSpecific);
	}

	public ConceptTreeNode<?> getElementByLocalId(int localId) {
		return localIdMap.get(localId);
	}
}
