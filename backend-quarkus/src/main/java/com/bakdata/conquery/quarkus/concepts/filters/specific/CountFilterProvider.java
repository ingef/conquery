package com.bakdata.conquery.quarkus.concepts.filters.specific;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "COUNT";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, "INTEGER_RANGE", 1, null, false, columns(column, optionalColumns(context, payload.distinctByColumn())));
	}
}
