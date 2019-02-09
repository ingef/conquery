package com.bakdata.conquery.models.identifiable.mapping;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespaceStorage;

import lombok.RequiredArgsConstructor;

@CPSType(base = IdMappingConfig.class, id = "NO_ID_MAPPING")
public class NoIdMapping extends IdMappingConfig {
	private final static String[] HEADER = new String[] {"result"};

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return new NoIdMappingAccessor[]{new NoIdMappingAccessor()};
	}

	@Override
	public String[] getPrintIdFields(){
		return HEADER;
	}

	@Override
	public String[] getHeader() {
		return HEADER;
	}

	private static class NoIdMappingAccessor implements IdMappingAccessor {
		@Override
		public String[] getHeader() {
			return HEADER;
		}

		@Override
		public boolean canBeApplied(List<String> csvHeader) {
			return true;
		}

		@Override
		public IdAccessor getApplicationMapping(String[] csvHeader, NamespaceStorage storage) {
			return new NoIdAccessor();
		}

		@Override
		public String[] extract(String[] dataLine) {
			return new String[]{dataLine[0]};
		}
	}

	@RequiredArgsConstructor
	private static class NoIdAccessor implements IdAccessor {

		@Override
		public CsvEntityId getCsvEntityId(String[] csvLine) {
			return new CsvEntityId(csvLine[0]);
		}
	}
}
