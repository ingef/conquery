package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;

@Data
public class FlagsAggregator extends Aggregator<Set<String>> {

	private final Map<String, Column> labels;

	private Set<String> result;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		result = new HashSet<>(labels.size());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		for (Map.Entry<String, Column> entry : labels.entrySet()) {

			final Column column = entry.getValue();
			final String label = entry.getKey();

			if (bucket.has(event, column) && bucket.getBoolean(event, column)) {
				result.add(label);
			}
		}
	}

	@Override
	public Set<String> createAggregationResult() {
		return result.isEmpty() ? null : result;
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.StringT.INSTANCE);
	}
}
