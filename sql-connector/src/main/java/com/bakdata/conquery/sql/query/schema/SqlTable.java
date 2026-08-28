package com.bakdata.conquery.sql.query.schema;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.sql.query.internal.ModelNormalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** A logical table together with its unquoted physical SQL name. */
public record SqlTable(
		@NotBlank String logicalId,
		@NotEmpty List<@NotBlank String> physicalName
) {

	public SqlTable {
		physicalName = ModelNormalization.immutableCopy(physicalName);
	}

	public static SqlTable of(String logicalId, String... physicalName) {
		if (physicalName == null) {
			return new SqlTable(logicalId, null);
		}
		return new SqlTable(logicalId, Arrays.asList(physicalName));
	}
}
