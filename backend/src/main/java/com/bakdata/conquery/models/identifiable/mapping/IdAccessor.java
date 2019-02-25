package com.bakdata.conquery.models.identifiable.mapping;

/**
 * A combination of together unique csv identifiers. Used for mapping parts of external Ids to csvId
 *
 */
public interface IdAccessor {
	/**
	 * Maps a line from a Csv to a CsvEntityId.
	 * @param csvLine A filtered line from a given Csv.
	 * @return the corresponding CsvEntityId.
	 */
	CsvEntityId getCsvEntityId(String[] csvLine);
}
