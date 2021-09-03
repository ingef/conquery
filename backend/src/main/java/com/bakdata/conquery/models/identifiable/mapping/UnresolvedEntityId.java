package com.bakdata.conquery.models.identifiable.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class UnresolvedEntityId {

	private final String externalId;
}
