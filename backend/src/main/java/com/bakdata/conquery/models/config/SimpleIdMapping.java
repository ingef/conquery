package com.bakdata.conquery.models.config;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.univocity.parsers.common.record.Record;

/**
 * Identity mapping.
 *
 * TODO should this actually map to a separate column?
 */
@CPSType(base = IdMappingConfig.class, id = "SIMPLE")
public class SimpleIdMapping extends IdMappingConfig {

	@Override
	protected void processRecord(Record record, CsvEntityId id, EntityIdMap mapping) {

		mapping.addOutputMapping(id, id.getCsvId());

		mapping.addInputMapping(id, id.getCsvId());

	}

	@Override
	public List<String> getPrintIdFields() {
		return List.of("result");
	}

}
