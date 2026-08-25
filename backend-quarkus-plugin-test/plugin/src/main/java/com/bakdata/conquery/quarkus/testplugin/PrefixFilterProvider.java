package com.bakdata.conquery.quarkus.testplugin;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixFilterProvider implements FilterDefinitionProvider<PrefixFilterDefinition> {

	@Override
	public Class<PrefixFilterDefinition> modelType() {
		return PrefixFilterDefinition.class;
	}

	@Override
	public Set<String> acceptedValueTypes() {
		return Set.of("STRING");
	}

	@Override
	public FilterResult convert(FilterConversionContext context, PrefixFilterDefinition payload) {
		ColumnDescriptor column = context.requireColumn(payload.getColumn());
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), "filter id", type());
		return new FilterResult(
				name,
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
				List.of(column.name())
		);
	}
}
