package com.bakdata.conquery.models.query.concept.filter;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import lombok.Data;

@Data
public class ValidityDate {
	@NotNull
	private ValidityDateId value;
}
