package com.bakdata.conquery.sql.translate.ir;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Condition;

/** Conditions grouped by the translate phase in which they must be applied. */
@Builder
@Value
public class WhereClauses {

	/** Connector and concept conditions applied while reading the raw connector table during preprocessing. */
	@Singular
	List<Condition> preprocessingConditions;

	/** User-selected filters evaluated for each raw event during preprocessing, before aggregation. */
	@Singular
	List<Condition> eventFilters;

	/** Filters evaluated after aggregation expressions have been calculated for an entity group. */
	@Singular
	List<Condition> groupFilters;

	public static WhereClauses empty() {
		return WhereClauses.builder().build();
	}
}
