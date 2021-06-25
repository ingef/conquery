package com.bakdata.conquery.models.config;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

@CPSType(base = IdMappingConfig.class, id = "NO_ID_MAPPING")
public class NoIdMapping extends IdMappingConfig {
	private static final String[] HEADER = new String[] {"result"};
	private static final NoIdMappingAccessor[] ACCESSORS = new NoIdMappingAccessor[]{NoIdMappingAccessor.INSTANCE};

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return ACCESSORS;
	}

	@Override
	public List<String> getPrintIdFields(){
		return Arrays.asList(HEADER);
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
		public IdAccessor getApplicationMapping(String[] csvHeader, final PersistentIdMap idMapping) {
			return NoIdAccessor.INSTANCE;
		}

		@Override
		public String[] extract(String[] dataLine) {
			return new String[]{dataLine[0]};
		}
		
		@Override
		public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
			return new CsvEntityId(reorderedCsvLine[0]);
		}

		@Override
		public int findIndexFromMappingHeader(String csvHeaderField) {
			return ArrayUtils.indexOf(getHeader(), csvHeaderField);
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
