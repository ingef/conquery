package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.IId;

public interface Named<ID extends IId<? extends Identifiable<? extends ID>>> extends Identifiable<ID> {

	String getName();
}
