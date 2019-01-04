package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID extends IId<? extends Identifiable<? extends ID>>> {

	@JsonIgnore @Valid
	ID getId();
}
