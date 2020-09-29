package com.bakdata.conquery.models.identifiable.mapping;

/**
 * Interface for classes that use to map the internal ids to readable, specific or even opaque (for pseudomization) ids and vice versa.
 * How internal and external ids look like depends on the individual implementation.
 * An important notice is, that multiple external ids can be mapped to a single internal id.
 * This is provided through the {@link IdMappingAccessor}s which can filter out id columns which work as "primary keys" but have the same uniqueness
 * to the entity as other "primary columns".
 */
public interface IdMapper {
	
	/**
	 * Map an internal id to an external.
	 */
	ExternalEntityId toExternal(CsvEntityId internal);
	
	/**
	 * Map an external to an internal id.
	 */
	CsvEntityId toInternal(SufficientExternalEntityId external);

	/**
	 * Add a mapping. The implementation looks after maintaining the mapping and inverse mapping.
	 */
	void addMapping(CsvEntityId internal, ExternalEntityId external, IdMappingAccessor[] idMappingAccessors);

}
