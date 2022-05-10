package com.bakdata.conquery.models.index;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface InternToExternMapper {

	void init();

	String external(String internalValue);
}
