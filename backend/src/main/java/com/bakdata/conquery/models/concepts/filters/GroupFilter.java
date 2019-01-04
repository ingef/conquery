package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.models.query.concept.filter.FilterValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public abstract class GroupFilter<FE_TYPE extends FilterValue<?>> extends Filter<FE_TYPE> {

	private static final long serialVersionUID = 1L;
	
	//public abstract Condition createGroupCondition(FE_TYPE qf);
	
}
