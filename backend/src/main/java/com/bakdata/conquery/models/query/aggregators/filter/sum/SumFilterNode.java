package com.bakdata.conquery.models.query.aggregators.filter.sum;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

public abstract class SumFilterNode<T> extends FilterNode<FilterValue<? extends IRange<?, ?>>, SumFilter> {

	private IRange<T, ?> range;
	private T sum;

	public SumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected final void init() {
		range = (IRange<T, ?>) filterValue.getValue();
		sum = defaultValue();
	}

	public abstract T defaultValue();

	@Override
	public final OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		T addend = getValue(block, event, filter.getColumn());

		sum = combine(sum, addend);

		return OpenResult.MAYBE;
	}

	protected abstract T getValue(Block block, int event, Column column);

	protected abstract T combine(T sum, T addend);

	@Override
	public final boolean isContained() {
		return range.contains(sum);
	}
}
