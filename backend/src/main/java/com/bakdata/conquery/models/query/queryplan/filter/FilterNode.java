package com.bakdata.conquery.models.query.queryplan.filter;

import java.util.Arrays;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FilterNode<FILTER_VALUE extends FilterValue<?>, FILTER extends Filter<FILTER_VALUE>> extends QPNode {

	protected final FILTER filter;
	protected final FILTER_VALUE filterValue;

	@Override
	public Multiset<Table> collectRequiredTables() {
		return HashMultiset.create(Arrays
										   .stream(filter.getRequiredColumns())
										   .map(Column::getTable)
										   .collect(ImmutableMultiset.toImmutableMultiset())
		);
	}
}
