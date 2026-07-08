package com.bakdata.conquery.quarkus.concepts.filters.specific;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DurationSumFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "DURATION_SUM";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, "INTEGER_RANGE", 0, null, false, columns(column, optionalColumns(context, payload.distinctBy())));
	}
}
