package com.bakdata.conquery.models.query.queryplan.filter;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.filters.ISingleColumnFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;

import lombok.Getter;
import lombok.Setter;

public abstract class SingleColumnFilterNode<FE_TYPE> extends FilterNode<FE_TYPE> implements ISingleColumnFilter {

	public SingleColumnFilterNode(Column column, FE_TYPE filterValue) {
		super(filterValue);
		this.column = column;
	}

	@NotNull @Getter @Setter
	private Column column;

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(column.getTable().getId());
	}
}
