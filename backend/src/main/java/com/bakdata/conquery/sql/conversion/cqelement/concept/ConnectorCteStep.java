package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConnectorCteStep implements CteStep {

	PREPROCESSING("preprocessing", null),
	EVENT_FILTER("event_filter", PREPROCESSING),
	AGGREGATION_SELECT("group_select", EVENT_FILTER),
	JOIN_BRANCHES("join_branches", AGGREGATION_SELECT),
	AGGREGATION_FILTER("group_filter", JOIN_BRANCHES);

	private static final Set<CteStep> STEPS = Set.of(values());

	private final String suffix;
	private final ConnectorCteStep predecessor;

	public static SqlTables createTables(String conceptConnectorLabel, String rootTable, NameGenerator nameGenerator) {
		return new SqlTables(conceptConnectorLabel, STEPS, rootTable, nameGenerator);
	}

}
