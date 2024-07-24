package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Cache for ConceptTree index searches.
 */
@RequiredArgsConstructor
@Data
public class ConceptTreeCache {

	/**
	 * Statistics for Cache.
	 */
	private int hits;
	/**
	 * Statistics for Cache.
	 */
	private int misses;

	@JsonIgnore
	private final TreeConcept treeConcept;

	/**
	 * Store of all cached values.
	 *
	 * @implNote ConcurrentHashMap does not allow null values, but we want to have null values in the map. So we wrap the values in Optional.
	 */
	@JsonIgnore
	private final Map<String, Optional<ConceptTreeChild>> cached = new ConcurrentHashMap<>();;


	/**
	 * If id is already in cache, use that. If not calculate it by querying treeConcept. If rowMap was not used to query, cache the response.
	 *
	 * @param value
	 */
	public ConceptTreeChild findMostSpecificChild(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {

		if(cached.containsKey(value)) {
			hits++;
			return cached.get(value).orElse(null);
		}

		misses++;

		final ConceptTreeChild child = treeConcept.findMostSpecificChild(value, rowMap);

		if(!rowMap.isCalculated()) {
			cached.put(value, Optional.ofNullable(child));
		}

		return child;
	}

}
