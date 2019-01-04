package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Abstract filter that includes entities when a counted property (ie a monotonically increasing value) is within a given range.
 *
 * @param <T>
 */
public abstract class CountingAbstractFilterNode<T extends Filter<FilterValue.CQIntegerRangeFilter>> extends FilterNode<FilterValue.CQIntegerRangeFilter, T> {

	private long value;

	public CountingAbstractFilterNode(T filter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected final OpenResult nextEvent(Block block, int event) {
		value = update(block, event);

		if (filterValue.getValue().isAll()) {
			return OpenResult.INCLUDED;
		}

		if (filterValue.getValue().getMax() < value) {
			return OpenResult.NOT_INCLUDED;
		}

		if (filterValue.getValue().isAtLeast() && filterValue.getValue().getMin() < value) {
			return OpenResult.INCLUDED;
		}

		return OpenResult.MAYBE;
	}

	protected abstract long update(Block block, int event);

	@Override
	public final boolean isContained() {
		return filterValue.getValue().contains(value);
	}
}
