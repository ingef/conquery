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
import lombok.Setter;


@AllArgsConstructor
public abstract class SingleColumnAggregator<T> extends ColumnAggregator<T> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@NsIdRef
	protected Column column;

	@Override
	public Column[] getRequiredColumns() {
		return new Column[] { getColumn() };
	}

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		out.add(getColumn().getTable().getId());
	}

	@Override
	public SingleColumnAggregator<T> clone(CloneContext ctx) {
		return ctx.clone(this);
	}
}
