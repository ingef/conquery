package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID extends AId<? extends Identifiable<? extends ID>>> {

	@JsonIgnore
	@Valid
	ID getId();
}
