package com.bakdata.conquery.models.concepts.tree.perf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroovyIndexPerformanceTest extends AbstractSearchPerformanceTest<String> {

	@Override
	public int[] getIterations(){
		return new int[]{1000, 10000, 100000};
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void postprocessConcepts() {
		TreeChildPrefixIndex.putIndexInto(newConcept);
	}

	@Override
	public String getConceptSourceName() {
		return "groovy.concept.json";
	}

	@Override
	public List<String> getTestKeys() {
		return Arrays.asList(
				"63F", "J14B", "N01C", "I10C", "L36Z", "960Z", "M10B", "X07A", "F06E", "P04C", "R63E", "O65B", "G77B", "F60B", "I65A", "F57Z", "R16Z", "R01D", "I23B", "A11E", "B44D", "F14A", "N62B", "Q61C", "I43B", "L43Z", "B36A", "F12F", "Z64B", "G07B"
		);
	}

	@Override
	public void referenceSearch(String key) throws ConceptConfigurationException {
		referenceConcept.findMostSpecificChild(key, new CalculatedValue<>(() -> Collections.singletonMap("distinction", RandomUtils.nextInt(8,19 ))));
	}

	@Override
	public void newSearch(String key) throws ConceptConfigurationException {
		newConcept.findMostSpecificChild(key, new CalculatedValue<>(() -> Collections.singletonMap("distinction", RandomUtils.nextInt(8,19 ))));
	}
}

