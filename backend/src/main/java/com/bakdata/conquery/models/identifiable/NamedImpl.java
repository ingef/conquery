package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.exceptions.validators.ValidName;
import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor
public abstract class NamedImpl<ID extends IId<? extends IdentifiableImpl<? extends ID>>> extends IdentifiableImpl<ID> implements Named<ID> {

	@ValidName @Getter(onMethod_=@Override) @Setter
	private String name;
}
