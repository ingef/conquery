package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.exceptions.validators.ValidName;
import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.*;

@AllArgsConstructor @NoArgsConstructor
@ToString
public abstract class NamedImpl<ID extends IId<? extends IdentifiableImpl<? extends ID>>> extends IdentifiableImpl<ID> implements Named<ID> {

	@ValidName
	@Getter(onMethod_ = @Override)
	@Setter
	@ToString.Include
	private String name;
}
