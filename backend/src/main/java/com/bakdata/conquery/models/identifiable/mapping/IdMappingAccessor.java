package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Arrays;
import java.util.List;

public interface IdMappingAccessor {

	String[] getHeader();

	/**
	 * Check whether all Fields used for the mapping by this id mapping accessor are
	 * present.
	 *
	 * @param csvHeader
	 *            List of the header Strings.
	 * @return whether the mapping can be applied to the header.
	 */
	default boolean canBeApplied(List<String> csvHeader) {
		for (String requiredHeader : getHeader()) {
			if (!csvHeader.contains(requiredHeader)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Retrieves an applicationMapping which maps from CsvHeader to IdMappingCsv
	 * Indices. Assumes that canBeApplied has been checked before and returned True.
	 *
	 * @param csvHeader
	 *            Array of the header Strings.
	 * @param idMapping
	 * @return The IdAccessor.
	 */
	default IdAccessor getApplicationMapping(String[] csvHeader, final PersistentIdMap idMapping) {
		int[] applicationMapping = new int[csvHeader.length];
		Arrays.fill(applicationMapping, -1);
		for (int indexInHeader = 0; indexInHeader < csvHeader.length; indexInHeader++) {
			String csvHeaderField = csvHeader[indexInHeader];
			int indexInCsvHeader = findIndexFromMappingHeader(csvHeaderField);
			if (indexInCsvHeader != -1) {
				applicationMapping[indexInHeader] = indexInCsvHeader;
			}
		}
		return new IdAccessorImpl(this, applicationMapping, idMapping);
	}
	
	/**
	 * Returns the index of the header in the setted mapping that maps best to the provided header in the CSV.
	 * @param csvHeaderField The header field in the CSV that is machted to the predefined required headers.
	 * @return The index of the predefined header that matched best.
	 */
	int findIndexFromMappingHeader(String csvHeaderField);

	/**
	 * Extracts the Id information from a CSV line using this accessor
	 * configuration.
	 *
	 * @param dataLine
	 *            A Line from a CSV.
	 * @return the dataLine without the unused fields.
	 */
	String[] extract(String[] dataLine);

	CsvEntityId getFallbackCsvId(String[] reorderedCsvLine);
}
