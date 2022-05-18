package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.AId;

public interface Named<ID extends AId<? extends Identifiable<? extends ID>>> extends Identifiable<ID> {

	String getName();
}
