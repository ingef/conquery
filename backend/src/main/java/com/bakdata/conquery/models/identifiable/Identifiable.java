package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID extends Id<? extends Identifiable<? extends ID>>> {

	@JsonIgnore
	@Valid
	ID getId();
}
