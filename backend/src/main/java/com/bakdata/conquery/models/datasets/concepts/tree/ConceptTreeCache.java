package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * Cache for ConceptTree index searches.
 */
public class ConceptTreeCache {

	/**
	 * Statistics for Cache.
	 */
	@Getter
	private int hits;
	/**
	 * Statistics for Cache.
	 */
	@Getter
	private int misses;

	@JsonIgnore
	private final TreeConcept treeConcept;

	/**
	 * Store of all cached values.
	 */

	@JsonIgnore
	private final Map<String, ConceptTreeChild> cached;

	public ConceptTreeCache(TreeConcept treeConcept) {
		this.treeConcept = treeConcept;
		cached = new HashMap<>();
	}

	/**
	 * If id is already in cache, use that. If not calculate it by querying treeConcept. If rowMap was not used to query, cache the response.
	 *
	 * @param value
	 */
	public ConceptTreeChild findMostSpecificChild(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {

		if(cached.containsKey(value)) {
			hits++;
			return cached.get(value);
		}

		misses++;

		final ConceptTreeChild child = treeConcept.findMostSpecificChild(value, rowMap);

		if(!rowMap.isCalculated()) {
			cached.put(value, child);
		}

		return child;
	}

}
