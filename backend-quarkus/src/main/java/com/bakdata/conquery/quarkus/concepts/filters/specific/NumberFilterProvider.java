package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NumberFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "NUMBER";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, numericFrontendType(context, column), null, null, false, List.of(column));
	}
}
