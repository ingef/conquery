package com.bakdata.conquery.models.config.search;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.core.setup.Environment;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM )
public interface SearchConfig {

	SearchProcessor createSearchProcessor(Environment environment, DatasetId id);
}
