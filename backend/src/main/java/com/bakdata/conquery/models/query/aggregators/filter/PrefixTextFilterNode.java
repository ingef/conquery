package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included once the value at the selected Column starts with the given string.
 */
public class PrefixTextFilterNode extends FilterNode<FilterValue.CQStringFilter, PrefixTextFilter> {

	public PrefixTextFilterNode(PrefixTextFilter prefixTextFilter, FilterValue.CQStringFilter filterValue) {
		super(prefixTextFilter, filterValue);
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new PrefixTextFilterNode(filter, filterValue);
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		int stringToken = block.getString(event, filter.getColumn());

		String value = (String) filter.getColumn().getTypeFor(block).createScriptValue(stringToken);

		if (value.startsWith(filterValue.getValue())) {
			return OpenResult.INCLUDED;
		}

		return OpenResult.MAYBE;
	}
}
