package com.bakdata.conquery.models.config;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.univocity.parsers.common.record.Record;

/**
 * Identity mapping.
 *
 * TODO should this actually map to a separate column?
 */
@CPSType(base = IdMappingConfig.class, id = "SIMPLE")
public class SimpleIdMapping extends IdMappingConfig {

	@Override
	protected void processRecord(Record record, String id, EntityIdMap mapping) {

		final EntityPrintId entityPrintId = EntityPrintId.from(id);

		mapping.addOutputMapping(id, entityPrintId);

		mapping.addInputMapping(id, id);

	}

	@Override
	public List<String> getPrintIdFields() {
		return List.of("result");
	}

}
