package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;

/**
 * Aggregator gathering all unique values in a column, into a Set.
 *
 * @param <VALUE> Value type of the column.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class AllValuesAggregator<VALUE> extends SingleColumnAggregator<List<VALUE>> {

	private final Set<VALUE> entries = new HashSet<>();

	public AllValuesAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		entries.clear();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (bucket.has(event, getColumn())) {
			entries.add((VALUE) bucket.createScriptValue(event, getColumn()));
		}
	}

	@Override
	public List<VALUE> createAggregationResult() {
		return entries.isEmpty() ? null : entries.stream().sorted().collect(Collectors.toList());
	}

}
