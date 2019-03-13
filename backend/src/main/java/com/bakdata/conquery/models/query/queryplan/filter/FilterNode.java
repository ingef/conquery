package com.bakdata.conquery.models.query.queryplan.filter;

import java.util.Set;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FilterNode<FILTER_VALUE> implements EventIterating {

	@Getter
	protected final FILTER_VALUE filterValue;

	@Override
	public abstract void collectRequiredTables(Set<TableId> requiredTables);

	public abstract FilterNode<?> clone(QueryPlan plan, QueryPlan clone);

	public boolean checkEvent(Block block, int event) {
		return true;
	}

	public abstract void acceptEvent(Block block, int event);

	public abstract boolean isContained();
}
