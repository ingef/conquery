package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator gathering all unique values in a column, into a Set.
 *
 * @param <VALUE> Value type of the column.
 */
public class AllValuesAggregator<VALUE> extends SingleColumnAggregator<Set<VALUE>> {

	private final Set<VALUE> entries = new HashSet<>();

	public AllValuesAggregator(Column column) {
		super(column);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (bucket.has(event, getColumn())) {
			entries.add((VALUE) bucket.createScriptValue(event, getColumn()));
		}
	}

	@Override
	public Set<VALUE> getAggregationResult() {
		return entries.isEmpty() ? null : entries;
	}

	@Override
	public AllValuesAggregator<VALUE> doClone(CloneContext ctx) {
		return new AllValuesAggregator<>(getColumn());
	}

	@Override
	public SimpleResultType getResultType() {
		return SimpleResultType.STRING;
	}
}
