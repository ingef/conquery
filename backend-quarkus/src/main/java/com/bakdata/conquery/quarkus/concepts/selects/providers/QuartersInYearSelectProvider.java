package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.QuartersInYearSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuartersInYearSelectProvider extends AbstractSingleColumnSelectProvider<QuartersInYearSelectDefinition> {

	public QuartersInYearSelectProvider() {
		super(QuartersInYearSelectDefinition.class);
	}

	@Override
	public String type() {
		return "QUARTERS_IN_YEAR";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, QuartersInYearSelectDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		requireColumnType(context, column, DatasetCatalogRepository.ColumnType.DATE, DatasetCatalogRepository.ColumnType.DATE_RANGE);
		return select(context, payload, primitive("INTEGER"), List.of(column));
	}
}
