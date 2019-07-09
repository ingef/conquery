package com.bakdata.conquery.models.concepts.tree.perf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrefixIndexPerformanceTest extends AbstractSearchPerformanceTest<String> {

	protected CalculatedValue<Map<String, Object>> rowMap;

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}


	@Override
	public List<String> getTestKeys() {
		return Arrays.asList(
				"A047b", "A0470", "A0471", "A0472", "A0473", "A0479", "A3180", "A3188", "A4151", "A4152", "A4158", "B2580", "B2588", "B3781", "B3788", "B9541", "B9542", "B9548", "B9590", "B9591", "B9681", "B9688", "C4101", "C4102", "C4130", "C4131", "C4132", "C7981", "C7982", "C7983", "C7984", "O029", "Z935", "M1202", "E011", "D370", "P13", "G259", "J9921", "I831", "H950", "E8331", "H511", "I11", "S252", "M0704"
		);
	}

	@Override
	public void postprocessConcepts() {
		rowMap = new CalculatedValue<>(Collections::emptyMap);
		TreeChildPrefixIndex.putIndexInto(newConcept);
	}

	@Override
	public void referenceSearch(String key) throws ConceptConfigurationException {
		referenceConcept.findMostSpecificChild(key, rowMap);
	}

	@Override
	public void newSearch(String key) throws ConceptConfigurationException {
		newConcept.findMostSpecificChild(key, rowMap);
	}

	@Override
	public String getConceptSourceName() {
		return "prefixes.concept.json";
	}
}

