package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class IntervalPackingTables {

	@Getter
	private final String validityDateSourceTableName;
	private final Map<IntervalPackingCteStep, String> cteNames;

	public static IntervalPackingTables forConcept(String nodeLabel, ConceptTables conceptTables, NameGenerator nameGenerator) {
		Map<IntervalPackingCteStep, String> cteNames = createCteNameMap(nodeLabel, nameGenerator);
		String preprocessingCteName = conceptTables.cteName(ConceptCteStep.PREPROCESSING);
		return new IntervalPackingTables(preprocessingCteName, cteNames);
	}

	public static IntervalPackingTables forGenericQueryStep(String nodeLabel, QueryStep predecessor, NameGenerator nameGenerator) {
		Map<IntervalPackingCteStep, String> cteNames = createCteNameMap(nodeLabel, nameGenerator);
		return new IntervalPackingTables(predecessor.getCteName(), cteNames);
	}

	public String cteName(IntervalPackingCteStep intervalPackingCteStep) {
		return this.cteNames.get(intervalPackingCteStep);
	}

	private static Map<IntervalPackingCteStep, String> createCteNameMap(String nodeLabel, NameGenerator nameGenerator) {
		return Arrays.stream(IntervalPackingCteStep.values())
					 .collect(Collectors.toMap(
							 Function.identity(),
							 step -> nameGenerator.cteStepName(nodeLabel, step)
					 ));
	}

}
