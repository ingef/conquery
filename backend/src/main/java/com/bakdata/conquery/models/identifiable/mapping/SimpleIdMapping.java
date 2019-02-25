package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(base= IdMappingConfig.class, id="SIMPLE")
public class SimpleIdMapping extends IdMappingConfig {

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return new IdMappingAccessor[]{new DefaultIdMappingAccessor(this, new int[]{0})};
	}

	@Override
	public String[] getPrintIdFields() {
		return new String[]{"result"};
	}

	@Override
	public String[] getHeader() {
		return new String[]{"id", "result"};
	}

}
