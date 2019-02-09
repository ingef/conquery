package com.bakdata.conquery.models.identifiable.mapping;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.NamespaceStorage;

import lombok.Getter;

/**
 *
 */
public class IdMappingAccessor {

	@Getter
	private final int[] idsUsed;
	@Getter
	private final IdMappingConfig mapping;

	public IdMappingAccessor(IdMappingConfig mapping, int[] idsUsed) {
		this.idsUsed = idsUsed;
		this.mapping = mapping;
	}

	@Override
	public String toString() {
		return "Using Fields: " + idsUsed;
	}

	/**
	 * Check whether all Fields used for the mapping by this id mapping accessor are present.
	 * @param csvHeader List of the header Strings.
	 * @return whether the mapping can be applied to the header.
	 */
	public boolean canBeApplied(List<String> csvHeader) {
		return mapping.getHeader().containsAll(csvHeader);
	}

	/**
	 * Retrieves an applicationMapping which maps from CsvHeader to IdMappingCsv Indices.
	 * Assumes that canBeApplied has been checked before and returned True.
	 * @param csvHeader Array of the header Strings.
	 * @param storage The Namespace Storage to use.
	 * @return The IdAccessor.
	 */
	public IdAccessorImpl getApplicationMapping(String[] csvHeader, NamespaceStorage storage) {
		int[] applicationMapping = new int[csvHeader.length];
		for (int indexInHeader = 0; indexInHeader < csvHeader.length; indexInHeader++) {
			String csvHeaderField = csvHeader[indexInHeader];
			int indexInCsvHeader = mapping.getHeader().indexOf(csvHeaderField);
			if (indexInCsvHeader != -1) {
				applicationMapping[indexInHeader] = indexInCsvHeader;
			}
		}
		return new IdAccessorImpl(this, applicationMapping, storage);
	}

	/**
	 *
	 * @param dataLine A Line from a CSV.
	 * @return the dataLine without the unused fields.
	 */
	public String[] extract(String[] dataLine) {
		String[] output = new String[idsUsed.length];
		for (int i = 0; i < idsUsed.length; i++) {
			output[i] = dataLine[idsUsed[i]];
		}
		return output;
	}

	public void updateMapping(PersistentIdMap mapping) {
		for (Map.Entry<CsvEntityId, ExternalEntityId> entry : mapping.getCsvIdToExternalIdMap().entrySet()) {
			mapping.getExternalIdPartCsvIdMap()
				.put(new SufficientExternalEntityId(this, extract(entry.getValue().getExternalId())), entry.getKey());
		}
	}
}
