package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.QuartersInYearFilterDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuartersInYearFilterProvider extends AbstractBuiltinFilterProvider<QuartersInYearFilterDefinition> {
	public QuartersInYearFilterProvider() {
		super(QuartersInYearFilterDefinition.class);
	}
	@Override
	public String type() {
		return "QUARTERS_IN_YEAR";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, QuartersInYearFilterDefinition payload) {
		return filter(context, payload, "INTEGER_RANGE", 1, 4, false, List.of(requiredColumn(context, payload)));
	}
}
