package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;

public interface NamespacedIdentifiable<ID extends Id<? extends NamespacedIdentifiable<? extends ID>> & NamespacedId> extends Identifiable<ID> {
	Dataset getDataset();
}
