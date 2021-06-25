package com.bakdata.conquery.models.identifiable;

import javax.validation.constraints.NotBlank;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public abstract class NamedImpl<ID extends IId<? extends IdentifiableImpl<? extends ID>>> extends IdentifiableImpl<ID> implements Named<ID> {

	@Getter(onMethod_ = {@Override, @ToString.Include, @NotBlank})
	@Setter
	private String name;
}
