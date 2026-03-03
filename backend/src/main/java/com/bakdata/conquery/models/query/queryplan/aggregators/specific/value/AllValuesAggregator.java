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
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;

/**
 * Aggregator gathering all unique values in a column, into a Set.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class AllValuesAggregator extends SingleColumnAggregator<List<String>> {

	private final Range.IntegerRange substring;
	private final Set<String> entries = new HashSet<>();

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
			entries.add(getSubstringFromRange(string, substring));
			return;
		}

		entries.add((String) bucket.createScriptValue(event, getColumn()));
	}

	@Override
	public List<String> createAggregationResult() {
		List<String> rendered = entries.stream().filter(Strings::isNotBlank).sorted().collect(Collectors.toList());
		return rendered.isEmpty() ? null : rendered;
	}

}
