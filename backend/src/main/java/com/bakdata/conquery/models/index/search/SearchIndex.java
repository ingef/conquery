package com.bakdata.conquery.models.index.search;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface SearchIndex extends Identifiable<SearchIndexId>, Named<SearchIndexId>, NamespacedIdentifiable<SearchIndexId> {

	@Override
	SearchIndexId getId();

	void setDataset(Dataset dataset);
}
