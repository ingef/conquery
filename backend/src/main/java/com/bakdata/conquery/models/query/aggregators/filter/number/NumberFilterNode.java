package com.bakdata.conquery.models.query.aggregators.filter.number;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

public abstract class NumberFilterNode<T extends Number> extends FilterNode<FilterValue<? extends IRange<? extends Number, ?>>, NumberFilter> {

	private IRange<T, ?> range;

	public NumberFilterNode(NumberFilter filter, FilterValue<? extends IRange<? extends Number, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected final void init() {
		range = (IRange<T, ?>) filterValue.getValue();
	}

	@Override
	public final OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		T eventValue = getValue(block, event, filter.getColumn());

		if (range.contains(eventValue)) {
			return OpenResult.INCLUDED;
		}
		else {
			return OpenResult.MAYBE;
		}
	}

	protected abstract T getValue(Block block, int event, Column column);
}
