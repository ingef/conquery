package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.MappableSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

abstract class AbstractMappableSelectProvider<T extends MappableSelectDefinition> extends AbstractSingleColumnSelectProvider<T> {

	protected AbstractMappableSelectProvider(Class<T> payloadType) {
		super(payloadType);
	}

	protected DatasetCatalogRepository.Select convertMapped(SelectConversionContext context, T payload) {
		ColumnId column = context.columnId(payload.getColumn());
		if (payload.getMapping() != null || payload.getSubstring() != null) {
			requireColumnType(context, column, ColumnType.STRING);
		}
		DatasetCatalogRepository.SelectResultType result = payload.getMapping() == null
				? resultType(context, column)
				: primitive("STRING");
		return select(context, payload, result, List.of(column));
	}
}
