package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Entity is included as when one column is equal to the selected value.
 */
@Slf4j
public class SelectFilterNode extends FilterNode<FilterValue.CQSelectFilter, AbstractSelectFilter<FilterValue.CQSelectFilter>> {
	private int selectedId = -1;

	public SelectFilterNode(AbstractSelectFilter filter, FilterValue.CQSelectFilter filterValue) {
		super(filter, filterValue);
	}

	@Override
	public FilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new SelectFilterNode(filter, filterValue);
	}

	@Override
	public void nextBlock(Block block) {
		try {
			selectedId = (Integer) filter.getColumn().getTypeFor(block).parse(filterValue.getValue());
		} catch (ParsingException e) {
			log.error("Failed to parse value '{}'", filterValue.getValue(), e);
		}
	}

	@Override
	public OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		int value = block.getString(event, filter.getColumn());

		return selectedId == value ? OpenResult.INCLUDED : OpenResult.MAYBE;
	}
}
