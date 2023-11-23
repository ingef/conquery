package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

public class IntervalPackingTables extends SqlTables<IntervalPackingCteStep> {

	public static final Set<IntervalPackingCteStep> REQUIRED_STEPS = Set.of(IntervalPackingCteStep.values());

	private IntervalPackingTables(String nodeLabel, Set<IntervalPackingCteStep> requiredSteps, String rootTableName) {
		super(nodeLabel, requiredSteps, rootTableName);
	}

	public static IntervalPackingTables forConcept(String nodeLabel, SqlTables<ConceptCteStep> conceptTables) {
		String preprocessingCteName = conceptTables.cteName(ConceptCteStep.PREPROCESSING);
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, preprocessingCteName);
	}

	public static IntervalPackingTables forGenericQueryStep(String nodeLabel, QueryStep predecessor) {
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, predecessor.getCteName());
	}

}
