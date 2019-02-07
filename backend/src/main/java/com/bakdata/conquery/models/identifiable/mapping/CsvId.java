package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Data;

@Data
public class CsvId {
	private final String csvId;

 	public static CsvId getFallbackCsvId(String[] idPart){
 		return new CsvId(String.join( "|", idPart));
	}
}