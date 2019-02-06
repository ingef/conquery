package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class PrefixTextAggregator extends SingleColumnAggregator<Set<String>> {

	private final Set<String> entries = new HashSet<>();
	private final String prefix;

	public PrefixTextAggregator(SelectId id, Column column, String prefix) {
		super(id, column);
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
		return new PrefixTextAggregator(getId(), getColumn(), prefix);
	}
}
