package com.bakdata.conquery.models.config;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.specific.external.FormatColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.IdColumn;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.univocity.parsers.common.record.Record;

/**
 * Maps input to itself for upload.
 */
@CPSType(base = IdMappingConfig.class, id = "SIMPLE")
public class SimpleIdMapping extends NoIdMapping {

	@Override
	public Map<String, FormatColumn> getFormatColumns() {
		final HashMap<String, FormatColumn> out = new HashMap<>(super.getFormatColumns());

		out.put(IdColumn.HANDLE, new IdColumn());

		return out;
	}

	@Override
	protected void processRecord(Record record, String id, EntityIdMap mapping) {

		final EntityPrintId entityPrintId = EntityPrintId.from(id);

		mapping.addOutputMapping(id, entityPrintId);

		mapping.addInputMapping(id, id);

	}
}
