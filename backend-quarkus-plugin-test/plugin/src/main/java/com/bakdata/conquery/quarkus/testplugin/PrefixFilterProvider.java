package com.bakdata.conquery.quarkus.testplugin;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.StringFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixFilterProvider implements FilterDefinitionProvider<PrefixFilterDefinition> {

	@Override
	public Class<PrefixFilterDefinition> modelType() {
		return PrefixFilterDefinition.class;
	}

	@Override
	public Set<Class<? extends FilterValue>> acceptedValueTypes() {
		return Set.of(StringFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, PrefixFilterDefinition payload) {
		ColumnId column = context.columnId(payload.getColumn());
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), "filter id", type());
		return new DatasetCatalogRepository.Filter(
				context.filterId(name),
				payload.getLabel(),
				"STRING",
				payload.getUnit(),
				payload.getTooltip(),
				List.of(),
				null,
				null,
				payload.getPrefix() + ".*",
				false,
				false,
				payload.getPrefix(),
				List.of(column)
		);
	}
}
