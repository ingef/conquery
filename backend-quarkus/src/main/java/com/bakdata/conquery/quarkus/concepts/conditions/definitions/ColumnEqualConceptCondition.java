package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataColumnEqualConceptCondition", description = "Matches when another column of a record/event has one of the configured values.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "COLUMN_EQUAL")
public final class ColumnEqualConceptCondition extends AbstractConceptCondition {

	@NotEmpty
	private List<String> values;

	@NotBlank
	private String column;
}
