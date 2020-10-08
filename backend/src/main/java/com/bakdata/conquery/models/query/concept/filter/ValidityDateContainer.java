package com.bakdata.conquery.models.query.concept.filter;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(onConstructor_ = @JsonCreator )
public class ValidityDateContainer {
	@NotNull
	private ValidityDateId value;
}
