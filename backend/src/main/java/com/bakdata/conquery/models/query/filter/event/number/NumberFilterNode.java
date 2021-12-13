package com.bakdata.conquery.models.query.filter.event.number;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Abstract class, filtering single events to be in a specified range. Entity is only included if a single event is in range.
 * There exist type specific implementations.
 * @param <RANGE> Range Type for inclusion test.
 */
@ToString(callSuper = true, of = "column")
public abstract class NumberFilterNode<RANGE extends IRange<?, ?>> extends EventFilterNode<RANGE> {


	@NotNull
	@Getter
	@Setter
	private Column column;

	public NumberFilterNode(Column column, RANGE filterValue) {
		super(filterValue);
		this.column = column;
	}

	@Override
	public final boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		return contains(bucket, event);
	}

	public abstract boolean contains(Bucket bucket, int event);

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}
}
