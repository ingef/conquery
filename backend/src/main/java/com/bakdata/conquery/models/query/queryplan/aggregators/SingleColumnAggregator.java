package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Defines an aggregator over just a single column.
 */
@AllArgsConstructor
public abstract class SingleColumnAggregator<T> extends ColumnAggregator<T> {

	@Valid
	@NotNull
	@Getter
	@NsIdRef
	protected final Column column;

	@Override
	public final Column[] getRequiredColumns() {
		return new Column[] { getColumn() };
	}

	@Override
	public final void collectRequiredTables(Set<TableId> out) {
		out.add(getColumn().getTable().getId());
	}

	@Override
	public SingleColumnAggregator<T> clone(CloneContext ctx) {
		return ctx.clone(this);
	}
}
