package com.bakdata.conquery.sql.model.schema;

import java.util.Arrays;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jooq.Name;
import org.jooq.impl.DSL;

/** A logical table together with its unquoted physical SQL name. */
public record SqlTable(
		@NotBlank String logicalId,
		@NotNull Name physicalName
) {

	public static SqlTable of(String logicalId, String... physicalName) {
		if (physicalName == null
				|| physicalName.length == 0
				|| Arrays.stream(physicalName).anyMatch(part -> part == null || part.isBlank())) {
			return new SqlTable(logicalId, null);
		}
		return new SqlTable(logicalId, DSL.name(physicalName));
	}

	@JsonIgnore
	@AssertTrue(message = "physicalName must contain at least one non-blank name part")
	public boolean isPhysicalNameValid() {
		return physicalName == null
				|| physicalName.getName().length > 0
				&& Arrays.stream(physicalName.getName()).allMatch(part -> part != null && !part.isBlank());
	}
}
