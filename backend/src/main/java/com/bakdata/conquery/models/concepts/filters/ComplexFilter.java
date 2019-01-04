package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.models.query.concept.filter.FilterValue;

public abstract class ComplexFilter<FE_TYPE extends FilterValue<?>> extends Filter<FE_TYPE> {

	private static final long serialVersionUID = 1L;
/*
	@Override
	public abstract Select<Record> generateComplexFilter(ComplexFilterExecutor exec, QueryContext context, FE_TYPE qf, Table<?> baseTable);
	
	@Override
	public Field<Object> select() {
		return FeatureSelector.NULL.select();
	}*/
}
