package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataColumnEqualConceptCondition", description = "Matches when another column of a record/event has one of the configured values.")
public final class ColumnEqualConceptCondition extends AbstractConceptCondition {

	@NotEmpty
	private List<String> values;

	@NotBlank
	private String column;
}
