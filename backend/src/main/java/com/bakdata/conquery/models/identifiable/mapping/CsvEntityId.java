package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Data;

/**
 * This class holds the csvId of an Entity.
 */
@Data
public class CsvEntityId implements EntityId {

	/**
	 * The csvId of an entity.
	 */
	private final String csvId;
}