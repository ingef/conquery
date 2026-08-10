package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DistinctSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistinctSelectProvider extends AbstractMappableSelectProvider<DistinctSelectDefinition> {

	public DistinctSelectProvider() {
		super(DistinctSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, DistinctSelectDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		if (payload.getMapping() != null || payload.getSubstring() != null) {
			requireColumnType(context, column, DatasetCatalogRepository.ColumnType.STRING);
		}
		DatasetCatalogRepository.SelectResultType elementType = payload.getMapping() == null
				? resultType(context, column)
				: primitive("STRING");
		return select(context, payload, DatasetCatalogRepository.SelectResultType.list(elementType), List.of(column));
	}
}
