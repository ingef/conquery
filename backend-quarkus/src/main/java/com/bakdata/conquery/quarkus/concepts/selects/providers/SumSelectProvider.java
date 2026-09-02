package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.SumSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SumSelectProvider extends AbstractSingleColumnSelectProvider<SumSelectDefinition> {

	public SumSelectProvider() {
		super(SumSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, SumSelectDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		requireColumnType(
				context,
				column,
				ColumnType.INTEGER,
				ColumnType.MONEY,
				ColumnType.DECIMAL,
				ColumnType.REAL
		);
		List<ColumnId> columns = new ArrayList<>(List.of(column));
		if (payload.getSubtractColumn() != null && !payload.getSubtractColumn().isBlank()) {
			ColumnId subtractColumn = context.columnId(payload.getSubtractColumn());
			if (context.columnType(subtractColumn) != context.columnType(column)) {
				throw new IllegalArgumentException("Select SUM columns must have the same type.");
			}
			columns.add(subtractColumn);
		}
		columns.addAll(optionalColumns(context, payload.getDistinctByColumn()));
		return select(context, payload, resultType(context, column), columns);
	}
}
