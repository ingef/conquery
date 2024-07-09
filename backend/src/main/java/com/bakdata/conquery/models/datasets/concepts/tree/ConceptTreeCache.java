package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * Cache for ConceptTree index searches.
 */
public class ConceptTreeCache {

	@JsonIgnore
	private final TreeConcept treeConcept;
	/**
	 * Store of all cached values.
	 */

	/**
	 * @implNote We are wrapping this in Optional because ConcurrentHashMap does not like null-values, but null is a real result denoting a miss.
	 */
	@JsonIgnore
	private final ConcurrentMap<String, Optional<ConceptElement<?>>> cached;
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

	public ConceptTreeCache(TreeConcept treeConcept) {
		this.treeConcept = treeConcept;
		cached = new ConcurrentHashMap<>();
	}

	/**
	 * If id is already in cache, use that. If not calculate it by querying treeConcept. If rowMap was not used to query, cache the response.
	 *
	 * @param value
	 */
	public ConceptElement<?> findMostSpecificChild(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {

		if (cached.containsKey(value)) {
			hits++;
			return cached.get(value).orElse(null);
		}

		misses++;

		final ConceptElement<?> child = treeConcept.findMostSpecificChild(value, rowMap);
		final Optional<ConceptElement<?>> out = Optional.ofNullable(child);

		if (!rowMap.isCalculated()) {
			cached.put(value, out);
		}

		return child;
	}

}
