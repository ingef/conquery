package com.bakdata.conquery.quarkus.concepts.selects.providers;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.FirstSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FirstSelectProvider extends AbstractMappableSelectProvider<FirstSelectDefinition> {

	public FirstSelectProvider() {
		super(FirstSelectDefinition.class);
	}

	@Override
	public String type() {
		return "FIRST";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, FirstSelectDefinition payload) {
		return convertMapped(context, payload);
	}
}
