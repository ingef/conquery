package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.CountQuartersSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountQuartersSelectProvider extends AbstractSelectProvider<CountQuartersSelectDefinition> {

	public CountQuartersSelectProvider() {
		super(CountQuartersSelectDefinition.class);
	}

	@Override
	public String type() {
		return "COUNT_QUARTERS";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, CountQuartersSelectDefinition payload) {
		List<ColumnId> columns = dateRangeColumns(context, payload);
		requireDateRangeTypes(context, columns);
		return select(context, payload, primitive("INTEGER"), columns);
	}
}
