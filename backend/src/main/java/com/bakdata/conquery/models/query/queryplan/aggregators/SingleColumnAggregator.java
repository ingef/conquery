package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines an aggregator over just a single column.
 */
@AllArgsConstructor
public abstract class SingleColumnAggregator<T> extends ColumnAggregator<T> {

	@Getter
	@Setter
	protected Column column;

	@Override
	public final Column[] getRequiredColumns() {
		return new Column[] { getColumn() };
	}

	@Override
	public final void collectRequiredTables(Set<Table> out) {
		out.add(getColumn().getTable());
	}

}
