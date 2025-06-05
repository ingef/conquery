package com.bakdata.conquery.models.query.filter.event;

import java.util.Set;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;

/**
 * Event is included when the value in column is one of many selected.
 */

@ToString(callSuper = true, of = "column")
public class SubstringMultiSelectFilterNode extends EventFilterNode<Set<String>> {

	private final Range.IntegerRange range;
	@NotNull
	@Getter
	@Setter
	private Column column;
	private final boolean empty;


	public SubstringMultiSelectFilterNode(Column column, Set<String> filterValue, Range.IntegerRange range) {
		super(filterValue);
		this.column = column;
		this.range = range;

		empty = filterValue.stream().anyMatch(Strings::isEmpty);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
	}

	@Override
	public void nextBlock(Bucket bucket) {
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {

		if (!bucket.has(event, getColumn())) {
			return empty;
		}

		final String string = bucket.getString(event, getColumn());

		final String substr = getSubstringFromRange(string, range);

		return getFilterValue().contains(substr);
	}

	//TODO move to util
	public static String getSubstringFromRange(String string, Range.IntegerRange range) {

		final int min = Math.max(0, range.getMin());
		final int max = Math.min(string.length() - 1, range.getMax());

		if (min > string.length()) {
			return "";
		}

		return string.substring(min, max);
	}


	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}
}
