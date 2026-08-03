package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.PrefixSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixSelectProvider extends AbstractSingleColumnSelectProvider<PrefixSelectDefinition> {

	public PrefixSelectProvider() {
		super(PrefixSelectDefinition.class);
	}

	@Override
	public String type() {
		return "PREFIX";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, PrefixSelectDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		requireColumnType(context, column, DatasetCatalogRepository.ColumnType.STRING);
		return select(context, payload, list("STRING"), List.of(column));
	}
}
