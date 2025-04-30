package com.bakdata.conquery.models.index.search;

import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public abstract class SearchIndex extends NamespacedIdentifiable<SearchIndexId> {
	@Getter
	@Setter
	private DatasetId dataset;

	@Getter
	@Setter
	@NotEmpty
	private String name;

	@Override
	public SearchIndexId createId() {
		return new SearchIndexId(getDataset(), getName());
	}
}
