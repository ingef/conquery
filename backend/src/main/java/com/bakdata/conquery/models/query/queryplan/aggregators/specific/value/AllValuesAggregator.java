package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import static com.bakdata.conquery.models.query.StringUtils.getSubstringFromRange;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import joptsimple.internal.Strings;
import lombok.ToString;

/**
 * Aggregator gathering all unique values in a column, into a Set.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class AllValuesAggregator extends SingleColumnAggregator<List<Object>> {

	private final Range.IntegerRange substring;
	private final Set<Object> entries = new HashSet<>();

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
			String extract = getSubstringFromRange(string, substring);

			if (Strings.isNullOrEmpty(extract)) {
				return;
			}

			entries.add(extract);
			return;
		}

		entries.add(bucket.createScriptValue(event, getColumn()));
	}

	@Override
	public List<Object> createAggregationResult() {
		List<Object> rendered = entries.stream().sorted().collect(Collectors.toList());
		return rendered.isEmpty() ? null : rendered;
	}

}
