package com.bakdata.conquery.apiv1.query.concept.filter;

import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator )
public class ValidityDateContainer {
	private final ValidityDateId value;
}
