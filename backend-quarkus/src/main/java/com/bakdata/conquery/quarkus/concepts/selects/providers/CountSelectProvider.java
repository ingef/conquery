package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.CountSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountSelectProvider extends AbstractSelectProvider<CountSelectDefinition> {

	public CountSelectProvider() {
		super(CountSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, CountSelectDefinition payload) {
		List<ColumnId> columns = new ArrayList<>();
		columns.add(context.columnId(payload.getColumn()));
		columns.addAll(optionalColumns(context, payload.getDistinctByColumn()));
		return select(context, payload, primitive("INTEGER"), columns);
	}
}
