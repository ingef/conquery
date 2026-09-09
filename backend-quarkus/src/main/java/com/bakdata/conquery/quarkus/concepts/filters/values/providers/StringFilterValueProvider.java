package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.StringFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StringFilterValueProvider extends AbstractFilterValueProvider<StringFilterValue> {
	public StringFilterValueProvider() {
		super(StringFilterValue.class);
	}

}
