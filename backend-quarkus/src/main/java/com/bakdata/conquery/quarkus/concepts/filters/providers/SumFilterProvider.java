package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SumFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SumFilterProvider extends AbstractFilterProvider<SumFilterDefinition> {
	public SumFilterProvider() {
		super(SumFilterDefinition.class, IntegerRangeFilterValue.class, MoneyRangeFilterValue.class, RealRangeFilterValue.class);
	}
	@Override
	public String type() {
		return "SUM";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, SumFilterDefinition payload) {
		ColumnId column = requiredColumn(context, payload);
		List<ColumnId> required = new ArrayList<>(columns(column, optionalColumns(context, payload.getDistinctByColumn())));
		if (payload.getSubtractColumn() != null && !payload.getSubtractColumn().isBlank()) {
			required.add(context.columnId(payload.getSubtractColumn()));
		}
		return filter(context, payload, numericRangeValueType(context, column), null, null, false, required);
	}
}
