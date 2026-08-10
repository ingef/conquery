package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import java.util.Map;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataFlagsFilter", description = "Multi-selection filter backed by one boolean column per option.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "FLAGS")
public final class FlagsFilterDefinition extends AbstractFilterDefinition {
	@NotEmpty
	@Schema(description = "Mapping from selectable labels to local boolean columns.", required = true)
	private Map<String, String> flags;
}
