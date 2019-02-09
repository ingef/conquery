package com.bakdata.conquery.models.identifiable.mapping;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespaceStorage;

import lombok.RequiredArgsConstructor;

@CPSType(base = IdMappingConfig.class, id = "NO_ID_MAPPING")
public class NoIdMapping extends IdMappingConfig {
	private static final String[] HEADER = new String[] {"result"};
	private static final NoIdMappingAccessor[] ACCESSORS = new NoIdMappingAccessor[]{NoIdMappingAccessor.INSTANCE};

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return ACCESSORS;
	}

	@Override
	public String[] getPrintIdFields(){
		return HEADER;
	}

	@Override
	public String[] getHeader() {
		return HEADER;
	}

	private enum NoIdMappingAccessor implements IdMappingAccessor {
		INSTANCE;
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
			return NoIdAccessor.INSTANCE;
		}

		@Override
		public String[] extract(String[] dataLine) {
			return new String[]{dataLine[0]};
		}
	}

	@RequiredArgsConstructor
	private enum NoIdAccessor implements IdAccessor {
		INSTANCE;
		@Override
		public CsvEntityId getCsvEntityId(String[] csvLine) {
			return new CsvEntityId(csvLine[0]);
		}
	}
}
