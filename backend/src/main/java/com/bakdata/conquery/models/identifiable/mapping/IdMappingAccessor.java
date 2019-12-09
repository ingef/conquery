package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import org.apache.commons.lang3.ArrayUtils;

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
	 * @param storage
	 *            The Namespace Storage to use.
	 * @return The IdAccessor.
	 */
	default IdAccessor getApplicationMapping(String[] csvHeader, NamespaceStorage storage) {
		int[] applicationMapping = new int[csvHeader.length];
		Arrays.fill(applicationMapping, -1);
		for (int indexInHeader = 0; indexInHeader < csvHeader.length; indexInHeader++) {
			String csvHeaderField = csvHeader[indexInHeader];
			int indexInCsvHeader = ArrayUtils.indexOf(getHeader(), csvHeaderField);
			if (indexInCsvHeader != -1) {
				applicationMapping[indexInHeader] = indexInCsvHeader;
			}
		}
		return new IdAccessorImpl(this, applicationMapping, storage);
	}

	/**
	 * @param dataLine
	 *            A Line from a CSV.
	 * @return the dataLine without the unused fields.
	 */
	String[] extract(String[] dataLine);

	CsvEntityId getFallbackCsvId(String[] reorderedCsvLine);
}
