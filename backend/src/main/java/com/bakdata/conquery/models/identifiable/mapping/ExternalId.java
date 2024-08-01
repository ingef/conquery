package com.bakdata.conquery.models.identifiable.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class ExternalId {
	private final String type;
	private final String id;
}
