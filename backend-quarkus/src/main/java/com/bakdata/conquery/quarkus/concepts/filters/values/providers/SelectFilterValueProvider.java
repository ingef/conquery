package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.SelectFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

// TODO maybe remove, it may has a different symantic to the frontend than StringFilterValueProvider  
@ApplicationScoped
public class SelectFilterValueProvider extends AbstractFilterValueProvider<SelectFilterValue> {
	public SelectFilterValueProvider() {
		super(SelectFilterValue.class);
	}

}
