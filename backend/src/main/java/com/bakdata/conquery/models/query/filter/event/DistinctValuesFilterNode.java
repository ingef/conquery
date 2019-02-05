package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Event is included only, when the value has not been seen before.
 */
public class DistinctValuesFilterNode<VALUE extends FilterValue<?>, FILTER extends Filter<VALUE>> extends FilterNode<VALUE, FILTER> {

	private boolean hit = false;

	private Set<Object> observed = new HashSet<>();

	@Getter
	private final Column column;

	public DistinctValuesFilterNode(FILTER filter, Column column) {
		super(filter, null);
		this.column = column;
	}


	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return false;
		}

		if (observed.add(block.getAsObject(event, getColumn()))) {
			hit = true;
			return true;
		}

		return false;
	}

	@Override
	public FilterNode<?, ?> clone(QueryPlan plan, QueryPlan clone) {
		return new DistinctValuesFilterNode<>(filter, getColumn());
	}

	@Override
	public void acceptEvent(Block block, int event) {
		this.hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
