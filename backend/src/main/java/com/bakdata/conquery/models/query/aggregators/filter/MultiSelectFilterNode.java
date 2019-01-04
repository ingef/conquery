package com.bakdata.conquery.models.query.aggregators.filter;

import java.util.Arrays;
import java.util.Set;

import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.google.common.collect.ImmutableSet;

import lombok.extern.slf4j.Slf4j;

/**
 * Includes entities when the specified column is one of many values.
 */
@Slf4j
public class MultiSelectFilterNode extends FilterNode<FilterValue.CQMultiSelectFilter, AbstractSelectFilter<FilterValue.CQMultiSelectFilter>> {

	private Set<Integer> selectedValues;

	public MultiSelectFilterNode(AbstractSelectFilter<FilterValue.CQMultiSelectFilter> multiSelectFilter, FilterValue.CQMultiSelectFilter filterValue) {
		super(multiSelectFilter, filterValue);
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new MultiSelectFilterNode(filter, filterValue);
	}

	@Override
	public void nextBlock(Block block) {
		selectedValues =
				Arrays
						.stream(filterValue.getValue())
						.map(v -> {
							try {
								return (Integer) filter.getColumn().getTypeFor(block).parse(v);
							} catch (ParsingException e) {
								log.error("Failed parsing value '{}'", v, e);
								throw new IllegalStateException(e);
							}
						})
						.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		if (selectedValues.contains(block.getString(event, filter.getColumn()))) {
			return OpenResult.INCLUDED;
		}

		return OpenResult.MAYBE;

	}
}
