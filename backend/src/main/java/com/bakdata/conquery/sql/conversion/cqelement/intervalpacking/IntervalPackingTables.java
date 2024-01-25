package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;


public class IntervalPackingTables extends SqlTables<IntervalPackingCteStep> {

	public static final Set<IntervalPackingCteStep> REQUIRED_STEPS = Set.of(IntervalPackingCteStep.values());

	private IntervalPackingTables(String nodeLabel, Set<IntervalPackingCteStep> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		super(nodeLabel, requiredSteps, rootTableName, nameGenerator);
	}

	public static IntervalPackingTables forConcept(String nodeLabel, SqlTables<ConnectorCteStep> conceptTables, NameGenerator nameGenerator) {
		String preprocessingCteName = conceptTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, preprocessingCteName, nameGenerator);
	}

	public static IntervalPackingTables forGenericQueryStep(String nodeLabel, QueryStep predecessor, NameGenerator nameGenerator) {
		return new IntervalPackingTables(nodeLabel, REQUIRED_STEPS, predecessor.getCteName(), nameGenerator);
	}

}
