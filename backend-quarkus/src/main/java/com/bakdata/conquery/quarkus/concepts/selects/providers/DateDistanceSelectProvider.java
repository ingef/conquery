package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DateDistanceSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DateDistanceSelectProvider extends AbstractSingleColumnSelectProvider<DateDistanceSelectDefinition> {

	public DateDistanceSelectProvider() {
		super(DateDistanceSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, DateDistanceSelectDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		requireColumnType(context, column, DatasetCatalogRepository.ColumnType.DATE, DatasetCatalogRepository.ColumnType.DATE_RANGE);
		return select(context, payload, primitive("INTEGER"), List.of(column));
	}
}
