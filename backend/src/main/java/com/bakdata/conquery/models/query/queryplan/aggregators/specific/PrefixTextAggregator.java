package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
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

		int stringToken = bucket.getString(event, getColumn());

		String value = (String) getColumn().getTypeFor(bucket).createScriptValue(stringToken);

		// if performance is a problem we could find the prefix once in the dictionary
		// and then only check the values
		if (value.startsWith(prefix)) {
			setHit();
			entries.add(value);
		}

	}

	@Override
	public Set<String> doGetAggregationResult() {
		return entries;
	}

	@Override
	public PrefixTextAggregator doClone(CloneContext ctx) {
		return new PrefixTextAggregator(getColumn(), prefix);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}
}
