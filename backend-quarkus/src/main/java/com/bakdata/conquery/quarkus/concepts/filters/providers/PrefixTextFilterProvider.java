package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.PrefixTextFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.StringFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixTextFilterProvider extends AbstractFilterProvider<PrefixTextFilterDefinition> {
	public PrefixTextFilterProvider() {
		super(PrefixTextFilterDefinition.class, StringFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, PrefixTextFilterDefinition payload) {
		return filter(context, payload, StringFilterValue.class, null, null, false, List.of(requiredColumn(context, payload)));
	}
}
