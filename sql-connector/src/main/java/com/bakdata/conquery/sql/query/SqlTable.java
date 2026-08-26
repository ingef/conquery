package com.bakdata.conquery.sql.query;

import java.util.Arrays;
import java.util.List;

/** A logical table together with its unquoted physical SQL name. */
public record SqlTable(String logicalId, List<String> physicalName) {

	public SqlTable {
		logicalId = ModelValidation.requireNonBlank(logicalId, "logicalId");
		ModelValidation.requireNotEmpty(physicalName, "physicalName");
		physicalName = List.copyOf(physicalName);
		physicalName.forEach(part -> ModelValidation.requireNonBlank(part, "physicalName part"));
	}

	public static SqlTable of(String logicalId, String... physicalName) {
		return new SqlTable(logicalId, Arrays.asList(physicalName));
	}
}
