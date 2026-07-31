package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.PrefixTextFilterDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixTextFilterProvider extends AbstractBuiltinFilterProvider<PrefixTextFilterDefinition> {
	public PrefixTextFilterProvider() {
		super(PrefixTextFilterDefinition.class);
	}
	@Override
	public String type() {
		return "PREFIX_TEXT";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, PrefixTextFilterDefinition payload) {
		return filter(context, payload, "STRING", null, null, false, List.of(requiredColumn(context, payload)));
	}
}
