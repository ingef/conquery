package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID extends IId<? extends Identifiable<? extends ID>>> {

	/**
	 * Enforces Type-Equality at compile time. As some API changes with {@link Object#equals(Object)} are caught very late.
	 */
	static <ID extends IId<T>, T extends Identifiable<ID>> boolean equalsById(T left, T right) {
		return IId.equals(left.getId(), right.getId());
	}

	@JsonIgnore @Valid
	ID getId();
}
