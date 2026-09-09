package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.SingleColumnSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

abstract class AbstractSingleColumnSelectProvider<T extends SingleColumnSelectDefinition> extends AbstractSelectProvider<T> {

	protected AbstractSingleColumnSelectProvider(Class<T> payloadType) {
		super(payloadType);
	}

	protected DatasetCatalogRepository.Select convertColumn(SelectConversionContext context, T payload) {
		ColumnId column = context.columnId(payload.getColumn());
		return select(context, payload, resultType(context, column), List.of(column));
	}
}
