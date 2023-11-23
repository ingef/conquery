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
	public void getPredecessorTableName(Set<ConceptCteStep> requiredSteps, ConceptCteStep step, String expectedPredecessorTableName) {
		ConceptTables conceptTables = new ConceptTables(CONCEPT_LABEL, requiredSteps, ROOT_TABLE);
		Assertions.assertEquals(
				expectedPredecessorTableName,
				conceptTables.getPredecessor(step)
		);
	}

	public static Stream<Arguments> requiredStepsProvider() {
		return Stream.of(

				// AGGREGATION_SELECT and FINAL direct predecessors missing
				Arguments.of(ConceptCteStep.MANDATORY_STEPS, ConceptCteStep.PREPROCESSING, ROOT_TABLE),
				Arguments.of(ConceptCteStep.MANDATORY_STEPS, ConceptCteStep.EVENT_FILTER, ConceptCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptCteStep.MANDATORY_STEPS, ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptCteStep.MANDATORY_STEPS, ConceptCteStep.AGGREGATION_FILTER, ConceptCteStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),
				Arguments.of(ConceptCteStep.MANDATORY_STEPS, ConceptCteStep.FINAL, ConceptCteStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),

				// only FINAL direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConceptCteStep.EVENT_FILTER)),
						ConceptCteStep.AGGREGATION_SELECT,
						ConceptCteStep.EVENT_FILTER.cteName(CONCEPT_LABEL)
				),

				// only AGGREGATION_SELECT direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConceptCteStep.AGGREGATION_FILTER)),
						ConceptCteStep.FINAL,
						ConceptCteStep.AGGREGATION_FILTER.cteName(CONCEPT_LABEL)
				),

				// more than 1 predecessor missing of FINAL
				Arguments.of(
						Set.of(ConceptCteStep.PREPROCESSING, ConceptCteStep.FINAL),
						ConceptCteStep.FINAL,
						ConceptCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)
				),

				// all predecessors missing of FINAL
				Arguments.of(
						Set.of(ConceptCteStep.FINAL),
						ConceptCteStep.FINAL,
						ROOT_TABLE
				)
		);
	}

	private static Set<ConceptCteStep> withAdditionalSteps(Set<ConceptCteStep> additionalSteps) {
		return Stream.concat(ConceptCteStep.MANDATORY_STEPS.stream(), additionalSteps.stream()).collect(Collectors.toSet());
	}

}
