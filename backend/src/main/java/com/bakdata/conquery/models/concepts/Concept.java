package com.bakdata.conquery.models.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(of={"name", "connectors"})
public class Concept extends ConceptElement<ConceptId> implements SelectHolder<UniversalSelect> {
	
	@Getter @Setter
	private boolean hidden = false;
	@JsonManagedReference @Valid @Getter @Setter
	private List<Connector> connectors=Collections.emptyList();
	@NotNull @Getter @Setter
	private DatasetId dataset;
	@Getter @Setter
	private int globalToLocalOffset;
	@JsonIgnore
	private transient int maxDepth=-1;
	@JsonIgnore
	private List<ConceptElement<?>> localIdMap = new ArrayList<>();
	@JsonIgnore @Getter
	private IdMap<ConceptTreeChildId, ConceptTreeChild> allChildren = new IdMap<>();
	@Getter @Setter
	private List<ConceptTreeChild> children = Collections.emptyList();
	@JsonIgnore @Getter @Setter
	private int localId;
	@JsonIgnore @Getter @Setter
	private int depth=-1;
	@NotNull @Getter @Setter @JsonManagedReference
	private List<UniversalSelect> selects = new ArrayList<>();
	@JsonIgnore @Getter @Setter
	private TreeChildPrefixIndex childIndex;
	@JsonIgnore
	private Map<ImportId, ConceptTreeCache> caches = new ConcurrentHashMap<>();
	
	public Connector getConnectorByName(String connector) {
		return connectors
				.stream()
				.filter(n->n.getName().equals(connector))
				.findAny()
				.orElseThrow(() -> new NoSuchElementException("Connector not found: " + connector));
	}

	public void initElements(Validator validator) throws ConfigurationException, JSONException {
		this.setLocalId(0);
		localIdMap.add(this);
		this.setDepth(-1);

		Set<ConstraintViolation<ConceptElement<?>>> errors = new HashSet<>();
		List<ConceptTreeChild> openList = new ArrayList<>();
		openList.addAll(this.getChildren());

		for(int i=0;i<openList.size();i++) {
			ConceptTreeChild ctc = openList.get(i);
			
			errors.addAll(validator.validate(ctc));
			
			try {
				ctc.setLocalId(localIdMap.size());
				localIdMap.add(ctc);
				allChildren.add(ctc);
				ctc.setDepth(ctc.getParent() == null ? 0 : ctc.getParent().getDepth() + 1);

				ctc.init();

			}
			catch(Exception e) {
				throw new RuntimeException("Error trying to consolidate the node "+ctc.getLabel()+" in "+this.getLabel(), e);
			}
			
			openList.addAll(openList.get(i).getChildren());
		}
		ValidatorHelper.failOnError(log, errors);
	}
	
	@Override @JsonIgnore
	public Concept getConcept() {
		return this;
	}
	
	@Override
	public ConceptId createId() {
		return new ConceptId(Objects.requireNonNull(dataset), getName());
	}
	
	@Override
	public long calculateBitMask() {
		return 0L;
	}
	
	@Override
	public Concept findConcept() {
		return this;
	}
	
	public ConceptTreeCache getCache(ImportId importId){
		return caches.get(importId);
	}
	
	@Override
	public int[] getPrefix() {
		return new int[0];
	}
	
	public int getMaxDepth() {
		if(maxDepth==-1) {
			maxDepth = allChildren.stream().mapToInt(ConceptElement::getDepth).max().orElse(-1) + 1;
		}

		return maxDepth;
	}
	
	public ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		if(this.getChildIndex() != null) {
			ConceptTreeChild best = this.getChildIndex().findMostSpecificChild(stringValue, rowMap);

			if(best != null) {
				return findMostSpecificChild(stringValue, rowMap, best, best.getChildren());
			}
		}

		return findMostSpecificChild(stringValue, rowMap, null, this.getChildren());
	}
	
	private ConceptTreeChild findMostSpecificChild(String stringValue, CalculatedValue<Map<String, Object>> rowMap, ConceptTreeChild best, List<ConceptTreeChild> currentList) throws ConceptConfigurationException {
		while(currentList != null && !currentList.isEmpty()) {
			ConceptTreeChild match = null;
			boolean failed = false;
			for (ConceptTreeChild n : currentList) {
				if (!n.getCondition().matches(stringValue, rowMap)) {
					continue;
				}

				if (match == null) {
					match = n;

					if(n.getChildIndex() != null) {
						ConceptTreeChild specificChild = n.getChildIndex().findMostSpecificChild(stringValue, rowMap);

						if(specificChild != null) {
							match = specificChild;
						}
					}
				}
				else {
					failed = true;
					log.error("Value '{}' matches the two nodes {} and {} in the tree {} (row={}))"
							, stringValue, match.getLabel(), n.getLabel(), n.getConcept().getLabel(), rowMap.getValue());
				}
			}

			if(failed) {
				return null;
			}
			else if(match != null) {
				best = match;
				currentList = match.getChildren();
			}
			else {
				break;
			}
		}
		return best;
	}
	
	public int countElements() {
		return 1 + allChildren.size();
	}
	
	/**
	 * Method to get the element of this concept tree that has the specified local ID.
	 * This should only be used by the query engine itself as an index.
	 * @param ids the local id array to look for
	 * @return the element matching the most specific local id in the array
	 */
	public ConceptElement<?> getElementByLocalId(@NonNull int[] ids) {
		int mostSpecific = ids[ids.length-1];
		return localIdMap.get(mostSpecific);
	}
	
	public void initializeIdCache(AStringType type, ImportId importId) {
		caches.computeIfAbsent(importId, id -> new ConceptTreeCache(this, type));
	}
	
	@Override
	public ConceptTreeChild getChildById(ConceptTreeChildId conceptTreeChildId) {
		return allChildren.getOrFail(conceptTreeChildId);
	}

	@JsonIgnore
	public boolean isTreeConcept() {
		return !children.isEmpty();
	}

	@Override
	public ConceptElement<?> getParent() {
		return null;
	}
}
