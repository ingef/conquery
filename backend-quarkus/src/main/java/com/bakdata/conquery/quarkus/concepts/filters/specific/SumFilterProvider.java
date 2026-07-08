package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SumFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "SUM";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		ColumnId column = requiredColumn(context, payload);
		List<ColumnId> required = new ArrayList<>(columns(column, optionalColumns(context, payload.distinctByColumn())));
		if (payload.subtractColumn() != null && !payload.subtractColumn().isBlank()) {
			required.add(context.columnId(payload.subtractColumn()));
		}
		return filter(context, payload, numericFrontendType(context, column), null, null, false, required);
	}
}
