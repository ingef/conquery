package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.FlagsSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FlagsSelectProvider extends AbstractSelectProvider<FlagsSelectDefinition> {

	public FlagsSelectProvider() {
		super(FlagsSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, FlagsSelectDefinition payload) {
		List<ColumnId> columns = payload.getFlags().values().stream().map(context::columnId).toList();
		columns.forEach(column -> requireColumnType(context, column, ColumnType.BOOLEAN));
		return select(context, payload, list("STRING"), columns);
	}
}
