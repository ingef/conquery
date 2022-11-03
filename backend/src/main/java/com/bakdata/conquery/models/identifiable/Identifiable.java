package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

public interface Identifiable<ID extends Id<? extends Identifiable<? extends ID>>> {

	@JsonIgnore
	@Valid
	@ToString.Include
	ID getId();
}
