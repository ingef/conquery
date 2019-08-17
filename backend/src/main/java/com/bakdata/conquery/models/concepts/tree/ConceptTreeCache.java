package com.bakdata.conquery.models.concepts.tree;

import java.util.Map;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;

import lombok.Getter;

public class ConceptTreeCache {

	@Getter
	private int hits;
	@Getter
	private int misses;

	@JsonIgnore
	private final Concept treeConcept;

	@JsonIgnore
	private final AStringType type;
	@JsonIgnore
	private final BitStore cached;
	@JsonIgnore
	private final ConceptTreeChild[] values;

	public ConceptTreeCache(Concept treeConcept, AStringType type) {
		this.type = type;
		this.treeConcept = treeConcept;
		
		values = new ConceptTreeChild[type.size()];
		cached = Bits.store(type.size());
	}

	public ConceptTreeChild findMostSpecificChild(int id, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		if(cached.getBit(id)) {
			hits++;
			return values[id];
		}

		misses++;

		String scriptValue = type.getElement(id);
		final ConceptTreeChild child = treeConcept.findMostSpecificChild(scriptValue, rowMap);

		if(!rowMap.isCalculated()) {
			cached.setBit(id, true);
			this.values[id] = child;
		}

		return child;
	}

}
