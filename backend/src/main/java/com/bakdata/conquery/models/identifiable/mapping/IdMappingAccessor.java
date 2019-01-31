package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class IdMappingAccessor {

	@Getter private final List<Integer> idsUsed = new ArrayList<>();
	@Getter private final IdMappingConfig mapping;

	public IdMappingAccessor(IdMappingConfig mapping, List<Integer> idsUsed) {
		this.idsUsed.addAll(idsUsed);
		this.mapping = mapping;
	}

	@Override public String toString() {
		return "Using Fields: " + idsUsed;
	}

	/*package*/ boolean canBeApplied(List<String> csvHeader) {
		return mapping.getHeader().containsAll(csvHeader);
	}

	/*package*/ IdAccessor getApplicationMapping(List<String> csvHeader) {
		// We assume canBeApplied has been checked before
		// applicationMapping maps from CsvHeader to IdMappingCsv Indices
		Map<Integer, Integer> applicationMapping = new HashMap<>();
		for (int indexInHeader = 0; indexInHeader < csvHeader.size(); indexInHeader++) {
			String csvHeaderField = csvHeader.get(indexInHeader);
			int indexInCsvHeader = mapping.getHeader().indexOf(csvHeaderField);
			if (indexInCsvHeader != -1) {
				applicationMapping.put(indexInHeader, indexInCsvHeader);
			}
		}
		return new IdAccessor(this, applicationMapping);
	}

	/*package*/ List<String> extract(List<String> dataLine) {
		List<String> output = new ArrayList<>();
		for (Integer index : idsUsed) {
			output.add(dataLine.get(index));
		}
		return output;
	}
}
