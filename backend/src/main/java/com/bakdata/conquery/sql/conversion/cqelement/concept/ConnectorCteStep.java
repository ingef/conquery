package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConnectorCteStep implements CteStep {

	PREPROCESSING("preprocessing", null),
	EVENT_FILTER("event_filter", PREPROCESSING),
	AGGREGATION_SELECT("group_select", EVENT_FILTER),
	JOIN_PREDECESSORS("join_predecessors", AGGREGATION_SELECT),
	AGGREGATION_FILTER("group_filter", JOIN_PREDECESSORS),
	FINAL("", AGGREGATION_FILTER);

	public static final Set<ConnectorCteStep>
			MANDATORY_STEPS =
			Set.of(ConnectorCteStep.PREPROCESSING, ConnectorCteStep.AGGREGATION_SELECT, ConnectorCteStep.FINAL);

	private final String suffix;
	private final ConnectorCteStep predecessor;

	public static Set<ConnectorCteStep> withOptionalSteps(ConnectorCteStep... connectorCteStep) {
		HashSet<ConnectorCteStep> steps = new HashSet<>(MANDATORY_STEPS);
		steps.addAll(Set.of(connectorCteStep));
		return steps;
	}

	@Override
	public String cteName(String conceptLabel) {
		return "%s-%s".formatted(conceptLabel, this.suffix);
	}

}
