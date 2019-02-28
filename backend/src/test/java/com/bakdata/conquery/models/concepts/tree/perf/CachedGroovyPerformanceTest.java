package com.bakdata.conquery.models.concepts.tree.perf;

import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CachedGroovyPerformanceTest extends AbstractSearchPerformanceTest<Integer> {

	private Dictionary dict;
	private ConceptTreeCache cache;
	private List<Integer> ids;

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int[] getIterations(){
		return new int[]{1000, 10000, 100000, 500000};
	}


	@Override
	public String getConceptSourceName() {
		return "prefixes.concept.json";
	}

	public List<String> getTestStrings() {
		return Arrays.asList(
				"63F", "J14B", "N01C", "I10C", "L36Z", "960Z", "M10B", "X07A", "F06E", "P04C", "R63E", "O65B", "G77B", "F60B", "I65A", "F57Z", "R16Z", "R01D", "I23B", "A11E", "B44D", "F14A", "N62B", "Q61C", "I43B", "L43Z", "B36A", "F12F", "Z64B", "G07B"
		);
	}

	@Override
	public List<Integer> getTestKeys() {
		return ids;
	}

	@Override
	public void postprocessConcepts() {
		dict = new Dictionary();

		ids = getTestStrings().stream().map(dict::add).collect(Collectors.toList());
		dict.compress();

		TreeChildPrefixIndex.putIndexInto(newConcept);
		TreeChildPrefixIndex.putIndexInto(referenceConcept);

		StringType type = new StringType();
		type.setDictionary(dict);
		newConcept.initializeIdCache(type, importId);

		cache = newConcept.getCache(importId);
	}

	@Override
	public void referenceSearch(Integer key) throws ConceptConfigurationException {
		referenceConcept.findMostSpecificChild(dict.getElement(key),  new CalculatedValue<>(() -> Collections.singletonMap("distinction", RandomUtils.nextInt(8, 19 ))));
	}

	@Override
	public void newSearch(Integer key) throws ConceptConfigurationException {
		cache.findMostSpecificChild(key,  new CalculatedValue<>(() -> Collections.singletonMap("distinction", RandomUtils.nextInt(8,19 ))));
	}

}

