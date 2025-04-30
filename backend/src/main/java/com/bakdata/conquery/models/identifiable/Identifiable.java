package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

public interface Identifiable<ID extends Id> {

	@JsonIgnore
	@ToString.Include
	ID getId();

}
