package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Data;

@Data
public class ExternalId {
	private final String[] externalId;

	static ExternalId fromCsvId(CsvId csvId){
		return new ExternalId(new String[]{csvId.getCsvId()});
	}
}
