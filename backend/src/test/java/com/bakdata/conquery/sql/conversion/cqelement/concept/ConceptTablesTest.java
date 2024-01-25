package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConceptTablesTest {

	private static final String CONCEPT_LABEL = "foo";
	private static final String ROOT_TABLE = "root";
	public static final int NAME_MAX_LENGTH = 127;
	private static final NameGenerator NAME_GENERATOR = new NameGenerator(NAME_MAX_LENGTH);

	@ParameterizedTest
	@MethodSource("requiredStepsProvider")
	public void getPredecessorTableName(Set<ConnectorCteStep> requiredSteps, ConnectorCteStep step, String expectedPredecessorTableName) {
		ConceptTables conceptTables = new ConceptTables(CONCEPT_LABEL, requiredSteps, ROOT_TABLE, NAME_GENERATOR);
		Assertions.assertEquals(
				expectedPredecessorTableName,
				conceptTables.getPredecessor(step)
		);
	}

	public static Stream<Arguments> requiredStepsProvider() {
		return Stream.of(

				// AGGREGATION_SELECT and FINAL direct predecessors missing
				Arguments.of(ConnectorCteStep.MANDATORY_STEPS, ConnectorCteStep.PREPROCESSING, ROOT_TABLE),
				Arguments.of(ConnectorCteStep.MANDATORY_STEPS, ConnectorCteStep.EVENT_FILTER, ConnectorCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConnectorCteStep.MANDATORY_STEPS, ConnectorCteStep.AGGREGATION_SELECT, ConnectorCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)),
				Arguments.of(ConnectorCteStep.MANDATORY_STEPS, ConnectorCteStep.AGGREGATION_FILTER, ConnectorCteStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),
				Arguments.of(ConnectorCteStep.MANDATORY_STEPS, ConnectorCteStep.FINAL, ConnectorCteStep.AGGREGATION_SELECT.cteName(CONCEPT_LABEL)),

				// only FINAL direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConnectorCteStep.EVENT_FILTER)),
						ConnectorCteStep.AGGREGATION_SELECT,
						ConnectorCteStep.EVENT_FILTER.cteName(CONCEPT_LABEL)
				),

				// only AGGREGATION_SELECT direct predecessor missing
				Arguments.of(
						withAdditionalSteps(Set.of(ConnectorCteStep.AGGREGATION_FILTER)),
						ConnectorCteStep.FINAL,
						ConnectorCteStep.AGGREGATION_FILTER.cteName(CONCEPT_LABEL)
				),

				// more than 1 predecessor missing of FINAL
				Arguments.of(
						Set.of(ConnectorCteStep.PREPROCESSING, ConnectorCteStep.FINAL),
						ConnectorCteStep.FINAL,
						ConnectorCteStep.PREPROCESSING.cteName(CONCEPT_LABEL)
				),

				// all predecessors missing of FINAL
				Arguments.of(
						Set.of(ConnectorCteStep.FINAL),
						ConnectorCteStep.FINAL,
						ROOT_TABLE
				)
		);
	}

	private static Set<ConnectorCteStep> withAdditionalSteps(Set<ConnectorCteStep> additionalSteps) {
		return Stream.concat(ConnectorCteStep.MANDATORY_STEPS.stream(), additionalSteps.stream()).collect(Collectors.toSet());
	}

}
