package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DurationSumSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DurationSumSelectProvider extends AbstractSelectProvider<DurationSumSelectDefinition> {

	public DurationSumSelectProvider() {
		super(DurationSumSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, DurationSumSelectDefinition payload) {
		List<ColumnId> rangeColumns = dateRangeColumns(context, payload);
		requireDateRangeTypes(context, rangeColumns);
		return select(context, payload, primitive("INTEGER"), append(rangeColumns, optionalColumns(context, payload.getDistinctBy())));
	}
}
