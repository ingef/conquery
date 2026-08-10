package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataCountFilter", description = "Range filter over an event or distinct-value count.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "COUNT")
public final class CountFilterDefinition extends SingleColumnFilterDefinition {
	@Schema(description = "Optional local columns whose value combinations are counted distinctly.")
	private List<String> distinctByColumn;

	// TODO remove this flag, decide if distinct "implicit" on value of distinctByColumn
	@Schema(description = "Whether the configured count column itself is counted distinctly.")
	private boolean distinct;
}
