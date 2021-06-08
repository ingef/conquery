package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.models.query.results.EntityResult;

@FunctionalInterface
/**
 * Maps an internalId to an external representation.
 */
public interface PrintIdMapper {
    ExternalEntityId map(EntityResult entityResult);
}
