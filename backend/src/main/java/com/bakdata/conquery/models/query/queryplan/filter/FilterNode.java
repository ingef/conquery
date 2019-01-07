package com.bakdata.conquery.models.query.queryplan.filter;

import java.util.Set;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FilterNode<FILTER_VALUE extends FilterValue<?>, FILTER extends Filter<FILTER_VALUE>> implements EventIterating {

	protected final FILTER filter;
	protected final FILTER_VALUE filterValue;

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for(Column c:filter.getRequiredColumns()) {
			requiredTables.add(c.getTable().getId());
		}
	}

	public abstract FilterNode<?,?> clone(QueryPlan plan, QueryPlan clone);

	public boolean checkEvent(Block block, int event) {
		return true;
	}

	public abstract void acceptEvent(Block block, int event);

	public abstract boolean isContained();
}
