package com.bakdata.conquery.sql.compiler.ir.condition;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** Conditions grouped by the compiler phase in which they must be applied. */
@Builder
@Value
public class WhereClauses {

	@Singular
	List<WhereCondition> preprocessingConditions;
	@Singular
	List<WhereCondition> eventFilters;
	@Singular
	List<WhereCondition> groupFilters;

	public static WhereClauses empty() {
		return WhereClauses.builder().build();
	}
}
