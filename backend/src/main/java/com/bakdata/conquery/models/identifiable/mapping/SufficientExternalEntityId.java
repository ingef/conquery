package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Data;

@Data
public class SufficientExternalEntityId implements EntityId{
	private final IdMappingAccessor idMappingAccessor;
	private final String[] externalIdPart;
}
