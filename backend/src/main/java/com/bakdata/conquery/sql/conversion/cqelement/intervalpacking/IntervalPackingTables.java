package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class IntervalPackingTables extends SqlTables<IntervalPackingStep> {

	public static final Set<IntervalPackingStep> REQUIRED_STEPS = Set.of(IntervalPackingStep.values());

	public IntervalPackingTables(String nodeLabel, Set<IntervalPackingStep> requiredSteps, String rootTableName) {
		super(nodeLabel, requiredSteps, rootTableName);
	}

	public static IntervalPackingTables forConcept(String nodeLabel, SqlTables<ConceptStep> conceptTables) {
		String preprocessingCteName = conceptTables.cteName(ConceptStep.PREPROCESSING);
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, preprocessingCteName);
	}

	public static IntervalPackingTables forGenericQueryStep(String nodeLabel, QueryStep predecessor) {
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, predecessor.getCteName());
	}

}
