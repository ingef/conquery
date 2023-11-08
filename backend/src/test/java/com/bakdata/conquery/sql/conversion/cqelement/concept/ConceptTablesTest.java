package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConceptTablesTest {

	private static final String CONCEPT_LABEL = "foo";
	private static final String ROOT_TABLE = "root";

	@ParameterizedTest
	@MethodSource("requiredStepsProvider")
	public void getPredecessorTableName(Set<ConceptStep> requiredSteps, ConceptStep step, String expectedPredecessorTableName) {
		ConceptTables conceptTables = new ConceptTables(CONCEPT_LABEL, requiredSteps, ROOT_TABLE);
		Assertions.assertEquals(
				expectedPredecessorTableName,
				conceptTables.getPredecessor(step)
		);
	}

	public static Stream<Arguments> requiredStepsProvider() {
		return Stream.of(

				// AGGREGATION_SELECT and FINAL direct predecessors missing
				Arguments.of(ConceptStep.MANDATORY_STEPS, ConceptStep.PREPROCESSING, ROOT_TABLE),
				Arguments.of(ConceptStep.MANDATORY_STEPS, ConceptStep.EVENT_FILTER, ConceptStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptStep.MANDATORY_STEPS, ConceptStep.AGGREGATION_SELECT, ConceptStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptStep.MANDATORY_STEPS, ConceptStep.AGGREGATION_FILTER, ConceptStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptStep.MANDATORY_STEPS, ConceptStep.FINAL, ConceptStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),

				// only FINAL direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConceptStep.EVENT_FILTER)),
						ConceptStep.AGGREGATION_SELECT,
						ConceptStep.EVENT_FILTER.cteName(CONCEPT_LABEL)
				),

				// only AGGREGATION_SELECT direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConceptStep.AGGREGATION_FILTER)),
						ConceptStep.FINAL,
						ConceptStep.AGGREGATION_FILTER.cteName(CONCEPT_LABEL)
				),

				// more than 1 predecessor missing of FINAL
				Arguments.of(
						Set.of(ConceptStep.PREPROCESSING, ConceptStep.FINAL),
						ConceptStep.FINAL,
						ConceptStep.PREPROCESSING.cteName(CONCEPT_LABEL)
				),

				// all predecessors missing of FINAL
				Arguments.of(
						Set.of(ConceptStep.FINAL),
						ConceptStep.FINAL,
						ROOT_TABLE
				)
		);
	}

	private static Set<ConceptStep> withAdditionalSteps(Set<ConceptStep> additionalSteps) {
		return Stream.concat(ConceptStep.MANDATORY_STEPS.stream(), additionalSteps.stream()).collect(Collectors.toSet());
	}

}
