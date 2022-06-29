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
 */
@ToString(callSuper = true, of = {"addendColumn", "subtrahendColumn"})
public class MultiDistinctValuesWrapperAggregator<VALUE> extends ColumnAggregator<VALUE> {

	private final ColumnAggregator<VALUE> aggregator;
	private Set<List<Object>> observed = new HashSet<>();

	@Getter
	private final Column[] columns;

	public MultiDistinctValuesWrapperAggregator(ColumnAggregator<VALUE> aggregator, Column[] columns) {
		this.columns = columns;
		this.aggregator = aggregator;
	}

	@Override
	public VALUE createAggregationResult() {
		return aggregator.createAggregationResult();
	}

	@Override
	public Column[] getRequiredColumns() {
		return ArrayUtils.addAll(aggregator.getRequiredColumns(), getColumns());
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		observed.clear();
		aggregator.init(entity, context);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		List<Object> tuple = new ArrayList<>(columns.length);
		for(Column column : columns) {
			if(!bucket.has(event,column)) {
				continue;
			}

			tuple.add(bucket.createScriptValue(event, column));
		}
		if (observed.add(tuple)) {
			aggregator.acceptEvent(bucket, event);
		}
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}