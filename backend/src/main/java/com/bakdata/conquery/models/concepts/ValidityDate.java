package com.bakdata.conquery.models.concepts;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ValidityDate extends Labeled<ValidityDateId> {

	@NsIdRef @NotNull
	private Column column;
	@JsonBackReference
	private Connector connector;
	
	@Override
	public ValidityDateId createId() {
		return new ValidityDateId(connector.getId(), getName());
	}
}