package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.Set;

import com.bakdata.conquery.sql.compiler.ir.CteStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Defines the connector-internal CTE graph used to compile a resolved concept node. */
@Getter
@AllArgsConstructor
public enum ConceptCteStep implements CteStep {

	PREPROCESSING("preprocessing", null),
	AGGREGATION_SELECT("group_select", PREPROCESSING),
	JOIN_BRANCHES("join_branches", AGGREGATION_SELECT),
	AGGREGATION_FILTER("group_filter", JOIN_BRANCHES),

	UNNEST_DATE("unnested", null),
	INTERVAL_PACKING_SELECTS("interval_packing_selects", null),

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
