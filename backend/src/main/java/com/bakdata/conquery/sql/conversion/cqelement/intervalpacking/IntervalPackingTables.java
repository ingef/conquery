package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class IntervalPackingTables {

	@Getter
	private final String validityDateSourceTableName;
	private final Map<IntervalPackingCteStep, String> cteNames;

	public static IntervalPackingTables forConcept(String nodeLabel, ConceptTables conceptTables) {
		Map<IntervalPackingCteStep, String> cteNames = Arrays.stream(IntervalPackingCteStep.values())
															 .collect(Collectors.toMap(
																	 Function.identity(),
																	 step -> "%s%s".formatted(nodeLabel, step.suffix())
															 ));
		String preprocessingCteName = conceptTables.cteName(ConceptCteStep.PREPROCESSING);
		return new IntervalPackingTables(preprocessingCteName, cteNames);
	}

	public String cteName(IntervalPackingCteStep intervalPackingCteStep) {
		return this.cteNames.get(intervalPackingCteStep);
	}

}
