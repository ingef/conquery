package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.Id;

public interface Named<ID extends Id<? extends Identifiable<? extends ID>>> extends Identifiable<ID> {

	String getName();
}
