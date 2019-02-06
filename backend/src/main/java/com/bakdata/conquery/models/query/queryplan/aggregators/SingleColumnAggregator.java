package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

public abstract class SingleColumnAggregator<T> extends ColumnAggregator<T> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@IdReference
	protected Column column;

	public SingleColumnAggregator(SelectId id, Column column) {
		super(id);
		setColumn(column);
	}

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		out.add(getColumn().getTable().getId());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{column};
	}

	@Override
	public abstract SingleColumnAggregator<T> clone();
}
