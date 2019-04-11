package com.bakdata.conquery.models.concepts.tree.perf;

import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class CachedPrefixPerformanceTest extends AbstractSearchPerformanceTest<Integer> {

	private final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(Collections::emptyMap);

	private Dictionary dict;
	private ConceptTreeCache cache;
	private List<Integer> ids;

	@Override
	public int[] getIterations(){
		return new int[]{1000, 10000, 100000, 500000};
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public String getConceptSourceName() {
		return "prefixes.concept.json";
	}

	public List<String> getTestStrings() {
		return Arrays.asList(
				"A047b", "A0470", "A0471", "A0472", "A0473", "A0479", "A3180", "A3188", "A4151", "A4152", "A4158", "B2580", "B2588", "B3781", "B3788", "B9541", "B9542", "B9548", "B9590", "B9591", "B9681", "B9688", "C4101", "C4102", "C4130", "C4131", "C4132", "C7981", "C7982", "C7983", "C7984", "O029", "Z935", "M1202", "E011", "D370", "P13", "G259", "J9921", "I831", "H950", "E8331", "H511", "I11", "S252", "M0704"
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
		referenceConcept.findMostSpecificChild(dict.getElement(key), rowMap);
	}

	@Override
	public void newSearch(Integer key) throws ConceptConfigurationException {
		cache.findMostSpecificChild(key, rowMap);
	}
}

