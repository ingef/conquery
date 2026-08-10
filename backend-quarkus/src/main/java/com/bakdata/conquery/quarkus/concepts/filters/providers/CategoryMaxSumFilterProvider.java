package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CategoryMaxSumFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryMaxSumFilterProvider extends AbstractFilterProvider<CategoryMaxSumFilterDefinition> {

	public CategoryMaxSumFilterProvider() {
		super(CategoryMaxSumFilterDefinition.class, IntegerRangeFilterValue.class, MoneyRangeFilterValue.class, RealRangeFilterValue.class);
	}


	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CategoryMaxSumFilterDefinition payload) {
		List<ColumnId> required = new ArrayList<>();
		ColumnId valueColumnId = context.columnId(payload.getValueColumn());
		required.add(valueColumnId);
		required.add(context.columnId(payload.getCategoryColumn()));
		return filter(context, payload, numericRangeValueType(context, valueColumnId), null, null, false, required);
	}
}
