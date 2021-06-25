package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.Map;

import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
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
	 * Stores if the value is present in the cache. Values are allowed to not be resolvable but we still want to cache the tree walk.
	 */
	@JsonIgnore
	private final BitStore cached;

	/**
	 * Store of all cached values.
	 */
	@JsonIgnore
	private final ConceptTreeChild[] values;

	public ConceptTreeCache(TreeConcept treeConcept, int size) {
		this.treeConcept = treeConcept;
		values = new ConceptTreeChild[size];
		cached = Bits.store(size);
	}

	/**
	 * If id is already in cache, use that. If not calculate it by querying treeConcept. If rowMap was not used to query, cache the response.
	 *
	 * @param id String id to resolve in conceptTree.
	 * @param scriptValue
	 */
	public ConceptTreeChild findMostSpecificChild(int id, String scriptValue, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {

		if(cached.getBit(id)) {
			hits++;
			return values[id];
		}

		misses++;

		final ConceptTreeChild child = treeConcept.findMostSpecificChild(scriptValue, rowMap);

		if(!rowMap.isCalculated()) {
			cached.setBit(id, true);
			this.values[id] = child;
		}

		return child;
	}

}
