package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.models.query.concept.filter.FilterValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public abstract class SimpleFilter<FE_TYPE extends FilterValue<?>> extends Filter<FE_TYPE> {

	private static final long serialVersionUID = 1L;
	
	/*@Override
	public abstract Condition createSimpleCondition(FE_TYPE qf);*/
}
