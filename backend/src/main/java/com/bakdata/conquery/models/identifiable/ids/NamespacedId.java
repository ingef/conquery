package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface NamespacedId {
	@JsonIgnore
	DatasetId getDataset();
}
