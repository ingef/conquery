package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.NamespaceStorage;

import lombok.Getter;

public class IdMappingAccessor {

	@Getter
	private final List<Integer> idsUsed = new ArrayList<>();
	@Getter
	private final IdMappingConfig mapping;

	public IdMappingAccessor(IdMappingConfig mapping, List<Integer> idsUsed) {
		this.idsUsed.addAll(idsUsed);
		this.mapping = mapping;
	}

	@Override public String toString() {
		return "Using Fields: " + idsUsed;
	}

	public boolean canBeApplied(List<String> csvHeader) {
		return mapping.getHeader().containsAll(csvHeader);
	}

	public IdAccessor getApplicationMapping(List<String> csvHeader, NamespaceStorage storage) {
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
		return new IdAccessor(this, applicationMapping, storage);
	}

	public String[] extract(String[] dataLine) {
		String[] output = new String[idsUsed.size()];
		for (int i = 0; i < idsUsed.size(); i++) {
			output[i] = dataLine[idsUsed.get(i)];
		}
		return output;
	}

	public void updateMapping(PersistingIdMap mapping) {
		for(Map.Entry<CsvId, ExternalId> entry: mapping.getCsvIdToExternalIdMap().entrySet()){
			mapping.getExternalIdPartCsvIdMap().put(new ExternalIdPart(this, extract(entry.getValue().getExternalId())), entry.getKey());
		}
	}
}
