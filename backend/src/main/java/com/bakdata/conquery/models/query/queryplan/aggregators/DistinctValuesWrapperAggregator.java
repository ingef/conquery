package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Helper Aggregator, forwarding only events with distinct values to {@code aggregator}.
 *
 * @param <VALUE>
 */
@ToString(callSuper = true, of = {"columns", "aggregator"})
public class DistinctValuesWrapperAggregator<VALUE> extends ColumnAggregator<VALUE> {

	private final ColumnAggregator<VALUE> aggregator;

	//TODO @ThoniTUB this can be gigantic, can I loosen this a bit and only scan for hashcodes?
	private final Set<List<Object>> observed = new HashSet<>();

	@Getter
	private final List<Column> columns;

	public DistinctValuesWrapperAggregator(ColumnAggregator<VALUE> aggregator, List<Column> columns) {
		this.columns = columns;
		this.aggregator = aggregator;
	}

	@Override
	public VALUE createAggregationResult() {
		return aggregator.createAggregationResult();
	}

	@Override
	public Column[] getRequiredColumns() {
		return ArrayUtils.addAll(aggregator.getRequiredColumns(), getColumns().toArray(Column[]::new));
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		aggregator.init(entity, context);
		observed.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		for (Column column : getColumns()) {
			if (!bucket.has(event, column)) {
				return;
			}
		}

		List<Object> incoming = new ArrayList<>(getColumns().size());

		for (Column column : getColumns()) {
			incoming.add(bucket.createScriptValue(event, column));
		}

		if (observed.add(incoming)) {
			aggregator.acceptEvent(bucket, event);
		}
	}

	@Override
	public ResultType getResultType() {
		return aggregator.getResultType();
	}

}