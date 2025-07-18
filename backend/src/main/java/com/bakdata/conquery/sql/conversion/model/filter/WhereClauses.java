package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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
