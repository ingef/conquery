package com.bakdata.conquery.models.index;


import java.net.URL;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

@CPSType(id = "CSV_MAP", base = InternToExternMapper.class)
public class MapInternToExternMapper implements InternToExternMapper {


	//Manager only
	@JsonIgnore
	private final Map<String, String> int2ext;


	@JsonCreator
	public MapInternToExternMapper(
			@JacksonInject Namespace namespace,
			URL csv,
			String internalColumn,
			String externalColumn
	) {
		if (namespace != null) {
			int2ext = namespace.getMapIndexService().getMapping(csv, internalColumn, externalColumn);
		}
		else {
			int2ext = null;
		}
	}


	@Override
	public String external(String internalValue) {
		return int2ext.getOrDefault(internalValue, "");
	}
}
