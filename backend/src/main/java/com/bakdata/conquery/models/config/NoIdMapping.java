package com.bakdata.conquery.models.config;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.univocity.parsers.common.record.Record;

/**
 * Disables Id-Mapping completely.
 */
public class NoIdMapping extends IdMappingConfig {
	private static final String[] HEADER = new String[]{"result"};


	@Override
	public List<String> getPrintIdFields() {
		return Arrays.asList(HEADER);
	}

}
