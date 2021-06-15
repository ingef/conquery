package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.DefaultIdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

@CPSType(base = IdMappingConfig.class, id = "SIMPLE")
public class SimpleIdMapping extends IdMappingConfig {

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return new IdMappingAccessor[]{
				new DefaultIdMappingAccessor(new int[]{0}, new String[]{"result"}) {
					@Override
					public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
						return new CsvEntityId(reorderedCsvLine[0]);
					}

					@Override
					public int findIndexFromMappingHeader(String csvHeaderField) {
						return ArrayUtils.indexOf(getHeader(), csvHeaderField);
					}
				}
		};
	}

	@Override
	public List<String> getPrintIdFields() {
		return List.of("result");
	}

	@Override
	public String[] getHeader() {
		return new String[]{"id", "result"};
	}

}
