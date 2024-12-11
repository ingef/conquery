package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public interface NamespacedIdentifiable<ID extends NamespacedId<? extends NamespacedIdentifiable<? extends ID>>> extends Identifiable<ID> {
	DatasetId getDataset();
}
