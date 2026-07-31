package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataFlagsSelect", description = "Returns labels for matching boolean flag columns.")
public final class FlagsSelectDefinition extends AbstractSelectDefinition {
	@NotEmpty
	private Map<String, String> flags;
}
