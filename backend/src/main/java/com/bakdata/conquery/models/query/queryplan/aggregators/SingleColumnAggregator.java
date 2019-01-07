package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public abstract class SingleColumnAggregator<T> implements Aggregator<T> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@IdReference
	protected Column column;

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		out.add(getColumn().getTable().getId());
	}

	@Override
	public abstract SingleColumnAggregator<T> clone();
}
