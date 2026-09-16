package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SingleSelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.SelectFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

// TODO remove this filter or migrate it's FilterValue to StringFilterValue
@ApplicationScoped
public class SingleSelectFilterProvider extends AbstractFilterProvider<SingleSelectFilterDefinition> {
	public SingleSelectFilterProvider() {
		super(SingleSelectFilterDefinition.class, SelectFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, SingleSelectFilterDefinition payload) {
		return filter(context, payload, SelectFilterValue.class, null, null, false, List.of(requiredColumn(context, payload)));
	}
}
