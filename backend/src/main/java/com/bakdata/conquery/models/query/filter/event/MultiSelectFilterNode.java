package com.bakdata.conquery.models.query.filter.event;

import java.util.Set;
import jakarta.validation.constraints.NotNull;

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
public class MultiSelectFilterNode extends EventFilterNode<Set<String>> {

	@NotNull
	@Getter
	@Setter
	private Column column;

	private final boolean empty;
	

	public MultiSelectFilterNode(Column column, Set<String> filterValue) {
		super(filterValue);
		this.column = column;
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

		final String stringToken = bucket.getString(event, getColumn());

		return getFilterValue().contains(stringToken);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}
	}
