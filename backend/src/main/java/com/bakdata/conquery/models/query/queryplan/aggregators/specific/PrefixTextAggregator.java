package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, returning all values of a column, beginning with a specified value.
 */
public class PrefixTextAggregator extends SingleColumnAggregator<Set<String>> {

	private final Set<String> entries = new HashSet<>();
	private final String prefix;

	public PrefixTextAggregator(Column column, String prefix) {
		super(column);
		this.prefix = prefix;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		String value = (String) bucket.createScriptValue(event, getColumn());

		// if performance is a problem we could find the prefix once in the dictionary
		// and then only check the values
		if (value.startsWith(prefix)) {
			entries.add(value);
		}

	}

	@Override
	public Set<String> getAggregationResult() {
		return entries.isEmpty() ? null : entries;
	}

	@Override
	public PrefixTextAggregator doClone(CloneContext ctx) {
		return new PrefixTextAggregator(getColumn(), prefix);
	}
	
	@Override
	public SimpleResultType getResultType() {
		return SimpleResultType.STRING;
	}
}
