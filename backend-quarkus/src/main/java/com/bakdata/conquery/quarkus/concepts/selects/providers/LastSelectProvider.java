package com.bakdata.conquery.quarkus.concepts.selects.providers;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.LastSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LastSelectProvider extends AbstractMappableSelectProvider<LastSelectDefinition> {

	public LastSelectProvider() {
		super(LastSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, LastSelectDefinition payload) {
		return convertMapped(context, payload);
	}
}
