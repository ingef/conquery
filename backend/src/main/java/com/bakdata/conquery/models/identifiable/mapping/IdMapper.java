package com.bakdata.conquery.models.identifiable.mapping;


public interface IdMapper {
	
	ExternalEntityId toExternal(CsvEntityId internal);
	
	CsvEntityId toInternal(SufficientExternalEntityId external);

	void addMapping(CsvEntityId internal, ExternalEntityId external, IdMappingAccessor[] idMappingAccessors);

}
