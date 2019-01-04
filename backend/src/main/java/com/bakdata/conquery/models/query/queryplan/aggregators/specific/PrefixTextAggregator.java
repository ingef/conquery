package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class PrefixTextAggregator extends SingleColumnAggregator<Set<String>> {

	private final Set<String> entries = new HashSet<>();
	private final String prefix;

	public PrefixTextAggregator(Column column, String prefix) {
		super(column);
		this.prefix = prefix;
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		int stringToken = block.getString(event, getColumn());

		String value = (String) getColumn().getTypeFor(block).createScriptValue(stringToken);

		// if performance is a problem we could find the prefix once in the dictionary
		// and then only check the values
		if (value.startsWith(prefix)) {
			entries.add(value);
		}

	}

	@Override
	public Set<String> getAggregationResult() {
		return entries;
	}

	@Override
	public PrefixTextAggregator clone() {
		return new PrefixTextAggregator(getColumn(), prefix);
	}
}
