package com.bakdata.conquery.quarkus.concepts.selects.providers;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.RandomSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RandomSelectProvider extends AbstractMappableSelectProvider<RandomSelectDefinition> {

	public RandomSelectProvider() {
		super(RandomSelectDefinition.class);
	}

	@Override
	public String type() {
		return "RANDOM";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, RandomSelectDefinition payload) {
		return convertMapped(context, payload);
	}
}
