package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.FlagsFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MultiSelectFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FlagsFilterProvider extends AbstractFilterProvider<FlagsFilterDefinition> {
	public FlagsFilterProvider() {
		super(FlagsFilterDefinition.class, MultiSelectFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, FlagsFilterDefinition payload) {
		return filter(context, payload, MultiSelectFilterValue.class, null, null, false, flagColumns(context, payload.getFlags()));
	}
}
