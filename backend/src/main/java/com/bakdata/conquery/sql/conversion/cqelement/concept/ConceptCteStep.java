
package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConceptCteStep implements CteStep {

	// connector
	PREPROCESSING("preprocessing", null), AGGREGATION_SELECT("group_select", PREPROCESSING), JOIN_BRANCHES(
		"join_branches",
		AGGREGATION_SELECT), AGGREGATION_FILTER("group_filter", JOIN_BRANCHES),

	// interval packing selects
	UNNEST_DATE("unnested", null), INTERVAL_PACKING_SELECTS("interval_packing_selects", null),

	// universal selects / final step
	UNIVERSAL_SELECTS("universal_selects", null);

	public static final Set<CteStep> MANDATORY_STEPS = Set.of(
		PREPROCESSING,
		AGGREGATION_SELECT,
		JOIN_BRANCHES,
		AGGREGATION_FILTER
	);

	private final String suffix;
	private final CteStep predecessor;

}
