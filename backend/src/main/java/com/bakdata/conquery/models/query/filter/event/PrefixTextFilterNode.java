package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class PrefixTextFilterNode extends FilterNode<FilterValue.CQStringFilter, PrefixTextFilter> {

	private final String prefix;
	private boolean hit;

	public PrefixTextFilterNode(PrefixTextFilter filter, FilterValue.CQStringFilter filterValue) {
		super(filter, filterValue);
		this.prefix = filterValue.getValue();
	}


	@Override
	public PrefixTextFilterNode doClone(CloneContext ctx) {
		return new PrefixTextFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		int stringToken = block.getString(event, filter.getColumn());

		String value = (String) filter.getColumn().getTypeFor(block).createScriptValue(stringToken);

		//if performance is a problem we could find the prefix once in the dictionary and then only check the values
		return value.startsWith(prefix);
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
