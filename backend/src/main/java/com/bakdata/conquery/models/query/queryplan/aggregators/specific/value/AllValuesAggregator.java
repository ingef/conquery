package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import static com.bakdata.conquery.models.query.filter.event.SubstringMultiSelectFilterNode.getSubstringFromRange;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;

/**
 * Aggregator gathering all unique values in a column, into a Set.
 *
 * @param <VALUE> Value type of the column.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class AllValuesAggregator<VALUE> extends SingleColumnAggregator<Set<VALUE>> {

	private final Range.IntegerRange substring;
	private final Set<VALUE> entries = new HashSet<>();

	public AllValuesAggregator(Column column, Range.IntegerRange substring) {
		super(column);
		this.substring = substring;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		entries.clear();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		if (substring != null) {
			String string = bucket.getString(event, getColumn());
			entries.add((VALUE) getSubstringFromRange(string, substring));
			return;
		}

		entries.add((VALUE) bucket.createScriptValue(event, getColumn()));
	}

	@Override
	public Set<VALUE> createAggregationResult() {
		return entries.isEmpty() ? null : ImmutableSet.copyOf(entries);
	}

}
