package com.bakdata.conquery.sql.compiler.ir.condition;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** Conditions grouped by the compiler phase in which they must be applied. */
@Builder
@Value
public class WhereClauses {

	/** Connector and concept conditions applied while reading t_e raw connector table during preprocessing. */
	@Singular
	List<WhereCondition> preprocessingConditions;

	/** User-selected filters evaluated for each raw event during preprocessing, before aggregation. */
	@Singular
	List<WhereCondition> eventFilters;

	/** Filters evaluated after aggregation expressions have been calculated for an entity group. */
	@Singular
	List<WhereCondition> groupFilters;

	public static WhereClauses empty() {
		return WhereClauses.builder().build();
	}
}
