package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPrefixSelect", description = "Returns values prefixed with a configured string.")
public final class PrefixSelectDefinition extends SingleColumnSelectDefinition {
	@NotNull
	private String prefix;
}
