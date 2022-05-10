package com.bakdata.conquery.models.index;


import java.net.URL;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@CPSType(id = "CSV_MAP", base = InternToExternMapper.class)
@RequiredArgsConstructor
public class MapInternToExternMapper implements InternToExternMapper {


	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private MapIndexService mapIndex;

	@Getter
	private final URL csv;
	@Getter
	private final String internalColumn;
	@Getter
	private final String externalColumn;


	//Manager only
	@JsonIgnore
	private Map<String, String> int2ext = null;


	@Override
	public void init() {
		if (mapIndex == null) {
			// Todo ensure we are on a Worker
			return;
		}
		// Manager
		int2ext = mapIndex.getMapping(csv, internalColumn, externalColumn);
	}


	@Override
	public String external(String internalValue) {
		return int2ext.getOrDefault(internalValue, "");
	}
}
