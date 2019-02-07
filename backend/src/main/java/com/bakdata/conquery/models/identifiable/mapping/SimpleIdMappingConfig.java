package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(base= IdMappingConfig.class, id="SIMPLE")
public class SimpleIdMappingConfig extends IdMappingConfig {

	@Override public List<IdMappingAccessor> getIdAccessors() {
		ArrayList<IdMappingAccessor> idAccessors = new ArrayList<>();
		idAccessors.add(new IdMappingAccessor(this, List.of(0)));
		return idAccessors;
	}

	@Override public List<String> getPrintIdFields() {
		return Collections.singletonList("result");
	}

	@Override public List<String> getHeader() {
		return Arrays.asList("id", "result");
	}

}
