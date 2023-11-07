package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class IntervalPackingTables extends SqlTables<IntervalPackingCteStep> {

	public static final Set<IntervalPackingCteStep> REQUIRED_STEPS = Arrays.stream(IntervalPackingCteStep.values()).collect(Collectors.toSet());

	public IntervalPackingTables(String nodeLabel, Set<IntervalPackingCteStep> requiredSteps, String rootTableName) {
		super(nodeLabel, requiredSteps, rootTableName);
	}

	public static IntervalPackingTables forConcept(String nodeLabel, ConceptTables conceptTables) {
		String preprocessingCteName = conceptTables.cteName(ConceptCteStep.PREPROCESSING);
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, preprocessingCteName);
	}

	public static IntervalPackingTables forGenericQueryStep(String nodeLabel, QueryStep predecessor) {
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, predecessor.getCteName());
	}

}
